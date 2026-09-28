#include <bits/stdc++.h>   // includes the whole standard library (works with GCC/MinGW)
using namespace std;

class ConfigService {
public:
    /*
    1) Clarifications
    about timestamps: Every method receives a timestamp. Timestamps are strictly increasing across calls.
    about nss (e.g., a service name, payments)): all differents, nss with 0 keys does not count
    about key (configurations e.g., timeout_ms): all different if we set two times the same, we replace it totally
    about value: (e.g, from timeout: value=1000 for example)

    2) Data structure choices
    vector -> good but storing but slow for fast lookups by key
    unordered_map: faster useful for the nss lookups, order here does not matter as much only if a requirement asked us for returning all the nss and its values sorted
    map: gives us order so it can be useful if in some moment we need to return the keys in the nss sorted
    */

    struct Entry {
        string value;
        int expiredAt; // -1 if it does not have ttl
        int remaining_time; // -1 if it does not have ttl
    };
    
    using Snapshot = unordered_map<string, map<string, Entry>>;
    unordered_map<string, map<string, Entry>> data;
    map<int, Snapshot> backups; // backup at 3 for example value: snapshot

    string format(const string& key, const string& value) {
        return key + "(" + value + ")";
    }

    bool isAlive(const Entry& e, int timestamp) {
        return timestamp < e.expiredAt || e.expiredAt==-1;
    }

    Entry* findAlive(int timestamp, const string& ns, const string& key) {
        auto it = data.find(ns);
        if (it==data.end()) return nullptr; // no namespace
        auto inner = it->second.find(key);
        if (inner == it->second.end() || !isAlive(inner->second, timestamp)) {
            return nullptr; // no key found
        }
        return &inner->second; // second is the entry
    }

    // saves a snapshot of all alive entries across all namespaces at that moment. It returns the number of entries saved.
    int backup(int timestamp) {
        Snapshot new_data;
        int entries_saved=0;
        for (auto& [name, inner]: data) {
            for(auto& [key, entry]: inner) {
                if(isAlive(entry, timestamp)) { // count and backup only alive entries
                    Entry copy = entry;
                    copy.remaining_time = (copy.expiredAt == -1) ? -1: copy.expiredAt - timestamp;
                    new_data[name][key] = copy;
                    entries_saved++;
                }
            }
        }
        backups[timestamp] = new_data;
        return entries_saved;
    }
    
    // finds the most recent backup taken at or before timestampToRestore and replaces all current data with it. 
    // It returns true if such a backup exists, or false otherwise (and leaves the data unchanged).
    bool restore(int timestamp, int timestampToRestore) {
        auto it = backups.upper_bound(timestampToRestore); // question this is as efficient as binary search? O(Lg N)?
        if(it == backups.begin()) return false;
        --it;
        Snapshot new_data;
        for(auto& [name, inner]: it->second) {
            for(auto& [key, entry]: inner) {
                // here we don't need to check if an entry is alive because we just stored alive entries
                Entry copy = entry;
                copy.expiredAt = (copy.expiredAt == -1) ? -1: timestamp + copy.remaining_time; // update time alive
                new_data[name][key] = copy;
            }
        }
        data = new_data;
        return true;
    }

    string setWithTtl(int timestamp, const string& ns, const string& key, const string& value, int ttl) {
        Entry* old = findAlive(timestamp, ns, key);
        string previous = (old) ? old->value : "";  // previous value , if it was not alive findAlive returns nullptr
        data[ns][key] = Entry{value, timestamp + ttl}; // set new_value with ttl
        return previous;
    }

    // stores value for the key in that ns. It returns the previous value, or "" if there wasn't one. infinite
    string set(int timestamp, const string& ns, const string& key, const string& value) {
        Entry* old = findAlive(timestamp, ns, key);
        string previous = (old) ? old->value : "";  // previous value
        data[ns][key] = Entry{value, -1}; // set new_value
        return previous;
    }

    // returns every entry in the namespace as "key(value)", sorted by key alphabetically.
    // If the namespace is missing or empty, it returns an empty list.
    vector<string> scanNamespace(int timestamp, const string& ns) {
        auto nsIt = data.find(ns);
        if (nsIt==data.end()) return {};
        vector<string> namespcs;
        auto& inner = nsIt->second;
        for (auto& [key, e]: inner) {
            if (isAlive(e, timestamp)){
                namespcs.push_back(format(key, e.value));
            }
        }
        return namespcs;
    }

    // works the same way, but only includes keys that start with prefix.
    vector<string> scanNamespaceByPrefix(int timestamp, const string& ns, const string& prefix) {
        auto nsIt = data.find(ns);
        if (nsIt==data.end()) return {};
        vector<string> namespcs;
        auto& inner = nsIt->second;
        for (auto it = inner.lower_bound(prefix); it != inner.end() && it->first.compare(0, prefix.size(), prefix) == 0; ++it) {
            if (isAlive(it->second, timestamp)){
                namespcs.push_back(format(it->first, it->second.value));
            }
        }
        return namespcs;
    }

    // returns the n namespaces with the most keys, as "namespace(count)".
    // Sort by count descending, then by name ascending on ties.
    // Namespaces with 0 keys never appear. If fewer than n exist, return all of them.
    vector<string> topNamespaces(int timestamp, int n) {
        vector<pair<int, string>> namespace_counts;
        for(auto& [name, inner]: data) {
            int cnt=0;
            for(auto& [key, entry]: inner ) {
                if(isAlive(entry, timestamp)) cnt++;
            }
            if (cnt>0) namespace_counts.push_back({cnt, name});
        }
        sort(namespace_counts.begin(), namespace_counts.end(), [](const auto& a, const auto& b) {
            if (a.first != b.first) return a.first > b.first;  // count descending
            return a.second < b.second;                         // name ascending
        });
        vector<string> result;
        for(auto& [cnt, name]: namespace_counts) {
            if(result.size() >= n) break;
            result.push_back(format(name, to_string(cnt)));
        }
        return result;
    }

    // returns the current value, or "" if the key doesn't exist in that ns.
    string get(int timestamp, const string& ns, const string& key) {
        Entry* e = findAlive(timestamp, ns, key);
        if (e == nullptr) return "";
        return e->value;
    }

    // deleteKey removes the key. It returns true if the key existed, or false otherwise.
    bool deleteKey(int timestamp, const string& ns, const string& key) {
        Entry* e = findAlive(timestamp, ns, key);
        if (e == nullptr) return false;
        data[ns].erase(key);
        if(data[ns].size() == 0) data.erase(ns); // delete namespace if it does not have keys left
        return true;
    }

    // updates the value to newValue only if the key exists and its current value equals expected.
    // It returns true if the update happened, or false otherwise.
    bool compareAndSet(int timestamp, const string& ns, const string& key, const string& expected, const string& newValue) {
        Entry* e = findAlive(timestamp, ns, key);
        if (e == nullptr) return false;
        if(e->value == expected) { // value equals expected, replace and return true
            e->value = newValue;
            return true;
        }
        return false; // value different than expected
    }
};

int main() {
    ConfigService s;
    int passed = 0, total = 0;

    // Compares a result with the expected value and prints PASS or FAIL
    auto check = [&](const string& label, const string& got, const string& expected) {
        total++;
        if (got == expected) { passed++; cout << "PASS " << label << "\n"; }
        else cout << "FAIL " << label << ": got \"" << got
                  << "\", expected \"" << expected << "\"\n";
    };
    // Turns a bool into "true"/"false" so check() can compare it
    auto b = [](bool v) { return string(v ? "true" : "false"); };

    check("1 set new",       s.set(1, "payments", "timeout_ms", "500"), "");
    check("2 set overwrite", s.set(2, "payments", "timeout_ms", "800"), "500");
    check("3 get",           s.get(3, "payments", "timeout_ms"), "800");
    check("4 get other ns",  s.get(4, "search", "timeout_ms"), "");
    check("5 cas wrong",     b(s.compareAndSet(5, "payments", "timeout_ms", "500", "900")), "false");
    check("6 cas right",     b(s.compareAndSet(6, "payments", "timeout_ms", "800", "900")), "true");
    check("7 delete",        b(s.deleteKey(7, "payments", "timeout_ms")), "true");
    check("8 delete again",  b(s.deleteKey(8, "payments", "timeout_ms")), "false");
    check("9 cas missing",   b(s.compareAndSet(9, "payments", "timeout_ms", "", "1")), "false");

    cout << "\n" << passed << "/" << total << " passed\n";

    // Turns a vector into "[a, b, c]" so check() can compare it
    auto v = [](const vector<string>& xs) {
        string r = "[";
        for (size_t i = 0; i < xs.size(); i++) { if (i) r += ", "; r += xs[i]; }
        return r + "]";
    };

    // ===== Level 2 (fresh service) =====
    ConfigService s2;
    check("L2 set 1", s2.set(1, "payments", "timeout_ms", "800"), "");
    check("L2 set 2", s2.set(2, "payments", "retries", "3"), "");
    check("L2 set 3", s2.set(3, "payments", "region", "us"), "");
    check("L2 set 4", s2.set(4, "search", "timeout_ms", "200"), "");
    check("L2 set 5", s2.set(5, "search", "cache", "on"), "");
    check("L2 set 6", s2.set(6, "auth", "ttl", "60"), "");
    check("L2 scan",          v(s2.scanNamespace(7, "payments")), "[region(us), retries(3), timeout_ms(800)]");
    check("L2 prefix",        v(s2.scanNamespaceByPrefix(8, "payments", "re")), "[region(us), retries(3)]");
    check("L2 scan missing",  v(s2.scanNamespace(9, "billing")), "[]");
    check("L2 top 2",         v(s2.topNamespaces(10, 2)), "[payments(3), search(2)]");
    check("L2 get ghost",     s2.get(11, "ghost", "x"), "");
    check("L2 top no ghost",  v(s2.topNamespaces(12, 5)), "[payments(3), search(2), auth(1)]");
    check("L2 top n=0",       v(s2.topNamespaces(13, 0)), "[]");
    check("L2 delete last",   b(s2.deleteKey(14, "auth", "ttl")), "true");
    check("L2 top no empty",  v(s2.topNamespaces(15, 5)), "[payments(3), search(2)]");
    check("L2 prefix none",   v(s2.scanNamespaceByPrefix(16, "payments", "zz")), "[]");
    check("L2 set 17",        s2.set(17, "flags", "a", "1"), "");
    check("L2 set 18",        s2.set(18, "flags", "b", "2"), "");
    check("L2 top tie",       v(s2.topNamespaces(19, 5)), "[payments(3), flags(2), search(2)]");

    // ===== Level 3 (fresh service) =====
    ConfigService s3;
    check("L3 set",             s3.set(1, "payments", "timeout_ms", "800"), "");
    check("L3 ttl promo",       s3.setWithTtl(2, "payments", "promo", "on", 5), "");
    check("L3 get alive",       s3.get(5, "payments", "promo"), "on");
    check("L3 scan alive",      v(s3.scanNamespace(6, "payments")), "[promo(on), timeout_ms(800)]");
    check("L3 get expired",     s3.get(7, "payments", "promo"), "");
    check("L3 scan expired",    v(s3.scanNamespace(8, "payments")), "[timeout_ms(800)]");
    check("L3 ttl beta",        s3.setWithTtl(9, "flags", "beta", "1", 3), "");
    check("L3 top tie",         v(s3.topNamespaces(10, 5)), "[flags(1), payments(1)]");
    check("L3 top expired ns",  v(s3.topNamespaces(12, 5)), "[payments(1)]");
    check("L3 ttl overwrite",   s3.setWithTtl(13, "payments", "timeout_ms", "900", 10), "800");
    check("L3 cas keeps ttl",   b(s3.compareAndSet(14, "payments", "timeout_ms", "900", "950")), "true");
    check("L3 get before exp",  s3.get(22, "payments", "timeout_ms"), "950");
    check("L3 get at exp",      s3.get(23, "payments", "timeout_ms"), "");
    check("L3 set over dead",   s3.set(24, "payments", "promo", "off"), "");
    check("L3 ttl retry_a",     s3.setWithTtl(25, "payments", "retry_a", "1", 2), "");
    check("L3 set retry_b",     s3.set(26, "payments", "retry_b", "2"), "");
    check("L3 prefix expired",  v(s3.scanNamespaceByPrefix(27, "payments", "retry")), "[retry_b(2)]");
    check("L3 delete expired",  b(s3.deleteKey(28, "payments", "retry_a")), "false");
    check("L3 ttl x",           s3.setWithTtl(29, "flags", "x", "1", 1), "");
    check("L3 cas expired",     b(s3.compareAndSet(30, "flags", "x", "1", "2")), "false");
    check("L3 ttl k",           s3.setWithTtl(31, "auth", "k", "a", 2), "");
    check("L3 set clears ttl",  s3.set(32, "auth", "k", "b"), "a");
    check("L3 get no ttl",      s3.get(40, "auth", "k"), "b");

        // ===== Level 4 (fresh service) =====
    ConfigService s4;
    check("L4 set",              s4.set(1, "payments", "timeout_ms", "800"), "");
    check("L4 ttl promo",        s4.setWithTtl(2, "payments", "promo", "on", 10), "");
    check("L4 ttl beta",         s4.setWithTtl(3, "flags", "beta", "1", 3), "");
    check("L4 backup 4",         to_string(s4.backup(4)), "3");
    check("L4 set 900",          s4.set(5, "payments", "timeout_ms", "900"), "800");
    check("L4 delete promo",     b(s4.deleteKey(6, "payments", "promo")), "true");
    check("L4 backup 7",         to_string(s4.backup(7)), "1");
    check("L4 restore to 5",     b(s4.restore(18, 5)), "true");
    check("L4 beta alive",       s4.get(19, "flags", "beta"), "1");
    check("L4 beta expired",     s4.get(20, "flags", "beta"), "");
    check("L4 old timeout",      s4.get(21, "payments", "timeout_ms"), "800");
    check("L4 promo alive",      s4.get(25, "payments", "promo"), "on");
    check("L4 promo expired",    s4.get(26, "payments", "promo"), "");
    check("L4 restore none",     b(s4.restore(30, 3)), "false");
    check("L4 data unchanged",   s4.get(31, "payments", "timeout_ms"), "800");
    check("L4 restore exact 7",  b(s4.restore(32, 7)), "true");
    check("L4 timeout 900",      s4.get(33, "payments", "timeout_ms"), "900");
    check("L4 promo not in 7",   s4.get(34, "payments", "promo"), "");
    check("L4 restore 5 again",  b(s4.restore(35, 5)), "true");
    check("L4 promo back",       s4.get(36, "payments", "promo"), "on");
    check("L4 modify after",     s4.set(37, "payments", "timeout_ms", "111"), "800");
    check("L4 restore 5 third",  b(s4.restore(38, 5)), "true");
    check("L4 backup untouched", s4.get(39, "payments", "timeout_ms"), "800");
    check("L4 top after",        v(s4.topNamespaces(40, 5)), "[payments(2)]");

    return 0;
}
