#include <bits/stdc++.h>   // includes the whole standard library (works with GCC/MinGW)
using namespace std;

class BankingSystem {
public:
    // data structure to keep accounts? hashmap or vector , hashmap is faster for lookups by accountId
    // unordered_map or map? unordered_map for now, if later a requirement ask for returning data sorted, I can change to map

    struct Account { // an struct for design, maybe later can change the requirements
        // account id could be another property but I will keep it as the key in the map
        int balance;
        int outgoing_money;
    };

    struct Payment {
        string account_id;
        int amount;
        string status; // PENDING, CANCELLED or DONE
    };

    unordered_map<string, Account> data; // accountId: Account
    unordered_map<string, Payment> payments; // what is Payment X (lookup by ID)
    map<int, vector<string>> pending; // pending payments (lookup by execution time) "What's due, in order?"
    int payment_count=1; // global order

    /*
    payments:  "payment1" -> {alice, 300, "PENDING"}
                "payment2" -> {alice, 200, "PENDING"}

    schedule:   8 -> ["payment2"]
                9 -> ["payment1"]
    */

    string format(const string& key, const string& value) {
        return key + '(' + value + ")";
    }

    /*
    This merges accountId2 into accountId1. It returns false (and changes nothing) if the two IDs are the same
    or either account doesn't exist. Otherwise it returns true, and:

    Balances combine: accountId1 gets accountId2's balance added to its own.
    Outgoing totals combine: accountId1's outgoing total becomes the sum of both.
    Pending payments move: accountId2's pending payments now belong to accountId1.
    They keep the same IDs and execution times, and must be cancelled through accountId1 from now on.
    accountId2 disappears: every method treats it as nonexistent, and topSpenders no longer lists it. A new account with that ID can be created later, starting fresh at balance 0 and outgoing 0.
    As always, process due payments first.
    */

    bool mergeAccounts(int timestamp, const string& accountId1, const string& accountId2) {
        processPayments(timestamp);
        Account* acnt_1 = findAccount(accountId1);
        Account* acnt_2 = findAccount(accountId2);
        if(acnt_1 == nullptr || acnt_2 == nullptr || acnt_1 == acnt_2) return false;
        acnt_1->balance += acnt_2->balance;
        acnt_1->outgoing_money += acnt_2->outgoing_money;

        // move pending payments
        for(auto& [paymentId, pm]: payments) { // this is O(P) where P is the number of payments, If we need to improve this in the future
            // we could implement another map or even a vector (using it as a bucket) that stores the paymentId 1 indexed and the vector<string> with all of its payments
            if(pm.account_id == accountId2 && pm.status == "PENDING") { // we have found a payment from account2 to move to account1
                pm.account_id = accountId1; // Assign new Id            }
            }
        }

        data.erase(accountId2);
        return true;
    }

    void processPayments(int timestamp) {
        while (!pending.empty() && pending.begin()->first <= timestamp) {
            auto& due = pending.begin()->second;   // vector<string>
            for (auto& id : due) {
                auto& p = payments[id]; // (reference, so status changes are saved)
                if(p.status != "PENDING") continue; // (it was cancelled)
                p.status = "DONE"; // whether it was succesfull or not
                // Find the account; if it has enough money, subtract from balance and add to outgoing_money
                Account* acnt = findAccount(p.account_id); // find most recent version of account
                if (acnt == nullptr || acnt->balance < p.amount) continue;
                acnt->balance -= p.amount;
                acnt->outgoing_money += p.amount;
            }
            pending.erase(pending.begin()); // well I think I don't need my manual pop(0) because here I am erasing it
        }
    }

    // schedules a withdrawal of amount to run at timestamp + delay.
    // It returns a payment ID: "payment1", "payment2", and so on, numbered globally across all accounts.
    // It returns "" if the account doesn't exist (and doesn't use up a number).
    string schedulePayment(int timestamp, const string& accountId, int amount, int delay) {
        processPayments(timestamp);
        Account* acnt = findAccount(accountId);
        if(acnt == nullptr) return ""; // account does not exist

        Payment pm = Payment{accountId, amount, "PENDING"};
        string payment_id = "payment"+to_string(payment_count);
        int execution_time = timestamp + delay;

        pending[execution_time].push_back(payment_id);
        payments[payment_id] = pm;

        payment_count++;
        return payment_id;
    }

    // cancels a pending payment. It returns true if it was cancelled, or false if the payment doesn't exist,
    // belongs to a different account, already ran, or was already cancelled.
    bool cancelPayment(int timestamp, const string& accountId, const string& paymentId) {
        processPayments(timestamp);
        auto it = payments.find(paymentId);
        if(it == payments.end()) return false; // payment does not exist
        if(it->second.account_id != accountId) return false; // payment belongs to different accountId
        if(it->second.status != "PENDING") return false; // If status DONE or already cancelled return false
        it->second.status = "CANCELLED";
        return true;
    }


    // returns the account's balance, or -1 if it doesn't exist.
    int getBalance(int timestamp, const string& accountId) {
        processPayments(timestamp);
        Account* acnt = findAccount(accountId);
        if(acnt == nullptr) return -1; // account did not exist
        return acnt->balance;
    }

    // removes amount and returns the new balance.
    // It returns -1 (and changes nothing) if the account doesn't exist or has insufficient funds.
    int withdraw(int timestamp, const string& accountId, int amount) {
        processPayments(timestamp);
        Account* acnt = findAccount(accountId);
        if(acnt == nullptr || acnt->balance < amount) return -1;
        acnt->balance -= amount;
        acnt->outgoing_money += amount;
        return acnt->balance;
    }

    // returns the n accounts with the highest total outgoing money, as "accountId(total)".
    // Outgoing means successful transfers sent plus successful withdrawals;
    // deposits and received transfers don't count. Sort by total descending, then by account ID ascending on ties.
    // Accounts with 0 outgoing are included. If fewer than n accounts exist, return all of them.
    vector<string> topSpenders(int timestamp, int n) {
        processPayments(timestamp);
        int i=0;
        vector<pair<int, string>> spenders;
        vector<string> top_spenders;

        for(auto& [account_id, acnt]: data) {
            spenders.push_back({acnt.outgoing_money, account_id});
        }
        sort(spenders.begin(), spenders.end(), [](const auto& a, const auto& b) {
            if (a.first != b.first) return a.first > b.first;  // outgoing money descending
            return a.second < b.second;                         // accountId ascending
        });

        for(auto& [outgoing_money, acntId]: spenders) {
            if(i>=n) break;
            top_spenders.push_back(format(acntId, to_string(outgoing_money)));
            i++;
        }
        return top_spenders;
    }


    Account* findAccount(const string& accountId) {
        auto it = data.find(accountId);
        if (it == data.end()) return nullptr;
        return &it->second;
    }

    // creates an account with balance 0. It returns true if created, or false if the account already exists.
    bool createAccount(int timestamp, const string& accountId) {
        processPayments(timestamp);
        Account* acnt = findAccount(accountId);
        if (acnt != nullptr) return false; // account already exists
        // account does not exist , acnt == nullptr
        data[accountId] = Account{0, 0};
        return true;
    }

    // adds amount to the account and returns the new balance, or -1 if the account doesn't exist.
    int deposit(int timestamp, const string& accountId, int amount) {
        processPayments(timestamp);
        Account* acnt = findAccount(accountId);
        if(acnt == nullptr) return -1; // account does not exist
        acnt->balance += amount;
        return acnt->balance;
    }

    // moves amount from source to target and returns the source's new balance.
    // It returns -1 (and changes nothing) if either account doesn't exist,
    // if source and target are the same account, or if the source has insufficient funds.
    int transfer(int timestamp, const string& sourceId, const string& targetId, int amount) {
        processPayments(timestamp);
        Account* source_acnt = findAccount(sourceId);
        Account* target_acnt = findAccount(targetId);
        if(source_acnt == nullptr) return -1; // source did not exists
        if(target_acnt == nullptr) return -1; // target did not exists
        if(source_acnt == target_acnt) return -1; // or just if sourceId == targetId same account
        if(amount > source_acnt->balance) return -1; // can't transfer more than what source have / insufficient funds
        // enough to transfer
        target_acnt->balance += amount;
        source_acnt->balance -= amount;
        source_acnt->outgoing_money += amount;
        return source_acnt->balance; // return source's new balance
    }
};


int main() {
    int passed = 0, total = 0;
    auto check = [&](const string& label, const string& got, const string& expected) {
        total++;
        if (got == expected) { passed++; cout << "PASS " << label << "\n"; }
        else cout << "FAIL " << label << ": got \"" << got
                  << "\", expected \"" << expected << "\"\n";
    };
    auto b = [](bool x) { return string(x ? "true" : "false"); };
    auto i = [](int x) { return to_string(x); };
    auto v = [](const vector<string>& xs) {
        string r = "[";
        for (size_t k = 0; k < xs.size(); k++) { if (k) r += ", "; r += xs[k]; }
        return r + "]";
    };

    // ===== Level 1 =====
    BankingSystem bank;
    check("L1 create alice",      b(bank.createAccount(1, "alice")), "true");
    check("L1 create dup",        b(bank.createAccount(2, "alice")), "false");
    check("L1 create bob",        b(bank.createAccount(3, "bob")), "true");
    check("L1 deposit",           i(bank.deposit(4, "alice", 1000)), "1000");
    check("L1 deposit again",     i(bank.deposit(5, "alice", 500)), "1500");
    check("L1 deposit missing",   i(bank.deposit(6, "carol", 100)), "-1");
    check("L1 transfer",          i(bank.transfer(7, "alice", "bob", 300)), "1200");
    check("L1 bob received",      i(bank.deposit(8, "bob", 50)), "350");
    check("L1 insufficient",      i(bank.transfer(9, "bob", "alice", 1000)), "-1");
    check("L1 same account",      i(bank.transfer(10, "alice", "alice", 10)), "-1");
    check("L1 missing target",    i(bank.transfer(11, "alice", "carol", 10)), "-1");
    check("L1 missing source",    i(bank.transfer(12, "dave", "alice", 10)), "-1");
    check("L1 exact balance",     i(bank.transfer(13, "bob", "alice", 350)), "0");
    check("L1 alice total",       i(bank.deposit(14, "alice", 1)), "1551");

    // ===== Level 2 (fresh bank) =====
    BankingSystem bank2;
    check("L2 create alice",     b(bank2.createAccount(1, "alice")), "true");
    check("L2 create bob",       b(bank2.createAccount(2, "bob")), "true");
    check("L2 create carol",     b(bank2.createAccount(3, "carol")), "true");
    check("L2 deposit alice",    i(bank2.deposit(4, "alice", 1000)), "1000");
    check("L2 deposit bob",      i(bank2.deposit(5, "bob", 500)), "500");
    check("L2 transfer a->b",    i(bank2.transfer(6, "alice", "bob", 200)), "800");
    check("L2 withdraw bob",     i(bank2.withdraw(7, "bob", 100)), "600");
    check("L2 withdraw broke",   i(bank2.withdraw(8, "carol", 10)), "-1");
    check("L2 withdraw missing", i(bank2.withdraw(9, "dave", 10)), "-1");
    check("L2 transfer b->c",    i(bank2.transfer(10, "bob", "carol", 300)), "300");
    check("L2 top 3",            v(bank2.topSpenders(11, 3)), "[bob(400), alice(200), carol(0)]");
    check("L2 transfer a->c",    i(bank2.transfer(12, "alice", "carol", 200)), "600");
    check("L2 top tie",          v(bank2.topSpenders(13, 2)), "[alice(400), bob(400)]");
    check("L2 failed transfer",  i(bank2.transfer(14, "carol", "alice", 10000)), "-1");
    check("L2 failed not count", v(bank2.topSpenders(15, 3)), "[alice(400), bob(400), carol(0)]");
    check("L2 top n=0",          v(bank2.topSpenders(16, 0)), "[]");
    check("L2 balance carol",    i(bank2.getBalance(17, "carol")), "500");
    check("L2 balance missing",  i(bank2.getBalance(18, "dave")), "-1");

    // ===== Level 3 (fresh bank) =====
    BankingSystem bank3;
    check("L3 create alice",     b(bank3.createAccount(1, "alice")), "true");
    check("L3 create bob",       b(bank3.createAccount(2, "bob")), "true");
    check("L3 deposit alice",    i(bank3.deposit(3, "alice", 1000)), "1000");
    check("L3 schedule p1",      bank3.schedulePayment(4, "alice", 300, 5), "payment1");
    check("L3 schedule p2",      bank3.schedulePayment(5, "alice", 200, 3), "payment2");
    check("L3 schedule missing", bank3.schedulePayment(6, "carol", 100, 1), "");
    check("L3 not yet",          i(bank3.getBalance(7, "alice")), "1000");
    check("L3 p2 ran at 8",      i(bank3.getBalance(8, "alice")), "800");
    check("L3 cancel too late",  b(bank3.cancelPayment(9, "alice", "payment1")), "false");
    check("L3 p1 ran at 9",      i(bank3.getBalance(10, "alice")), "500");
    check("L3 schedule p3",      bank3.schedulePayment(11, "alice", 100, 5), "payment3");
    check("L3 cancel wrong acct",b(bank3.cancelPayment(12, "bob", "payment3")), "false");
    check("L3 cancel p3",        b(bank3.cancelPayment(13, "alice", "payment3")), "true");
    check("L3 cancel twice",     b(bank3.cancelPayment(14, "alice", "payment3")), "false");
    check("L3 p3 never ran",     i(bank3.getBalance(16, "alice")), "500");
    check("L3 schedule p4",      bank3.schedulePayment(17, "bob", 50, 1), "payment4");
    check("L3 p4 skipped",       i(bank3.getBalance(18, "bob")), "0");
    check("L3 top spenders",     v(bank3.topSpenders(19, 2)), "[alice(500), bob(0)]");
    check("L3 deposit bob",      i(bank3.deposit(20, "bob", 100)), "100");
    check("L3 schedule p5",      bank3.schedulePayment(21, "bob", 80, 4), "payment5");
    check("L3 schedule p6",      bank3.schedulePayment(22, "bob", 50, 3), "payment6");
    check("L3 tie order",        i(bank3.getBalance(25, "bob")), "20");

        // ===== Level 4 (fresh bank) =====
    BankingSystem bank4;
    check("L4 create alice",     b(bank4.createAccount(1, "alice")), "true");
    check("L4 create bob",       b(bank4.createAccount(2, "bob")), "true");
    check("L4 create carol",     b(bank4.createAccount(3, "carol")), "true");
    check("L4 deposit alice",    i(bank4.deposit(4, "alice", 500)), "500");
    check("L4 deposit bob",      i(bank4.deposit(5, "bob", 300)), "300");
    check("L4 transfer b->c",    i(bank4.transfer(6, "bob", "carol", 100)), "200");
    check("L4 withdraw alice",   i(bank4.withdraw(7, "alice", 50)), "450");
    check("L4 schedule p1",      bank4.schedulePayment(8, "bob", 150, 10), "payment1");
    check("L4 merge self",       b(bank4.mergeAccounts(9, "alice", "alice")), "false");
    check("L4 merge missing 2",  b(bank4.mergeAccounts(10, "alice", "dave")), "false");
    check("L4 merge missing 1",  b(bank4.mergeAccounts(11, "dave", "alice")), "false");
    check("L4 merge bob",        b(bank4.mergeAccounts(12, "alice", "bob")), "true");
    check("L4 alice combined",   i(bank4.getBalance(13, "alice")), "650");
    check("L4 bob gone",         i(bank4.getBalance(14, "bob")), "-1");
    check("L4 deposit bob gone", i(bank4.deposit(15, "bob", 10)), "-1");
    check("L4 cancel via bob",   b(bank4.cancelPayment(16, "bob", "payment1")), "false");
    check("L4 top after merge",  v(bank4.topSpenders(17, 5)), "[alice(150), carol(0)]");
    check("L4 p1 from alice",    i(bank4.getBalance(18, "alice")), "500");
    check("L4 p1 outgoing",      v(bank4.topSpenders(19, 1)), "[alice(300)]");
    check("L4 recreate bob",     b(bank4.createAccount(20, "bob")), "true");
    check("L4 new bob empty",    i(bank4.getBalance(21, "bob")), "0");
    check("L4 top with new bob", v(bank4.topSpenders(22, 5)), "[alice(300), bob(0), carol(0)]");
    check("L4 schedule p2",      bank4.schedulePayment(23, "carol", 50, 10), "payment2");
    check("L4 merge carol",      b(bank4.mergeAccounts(24, "alice", "carol")), "true");
    check("L4 cancel via carol", b(bank4.cancelPayment(25, "carol", "payment2")), "false");
    check("L4 cancel via alice", b(bank4.cancelPayment(26, "alice", "payment2")), "true");
    check("L4 p2 never ran",     i(bank4.getBalance(35, "alice")), "600");

    cout << "\n" << passed << "/" << total << " passed\n";
    return 0;
}
