#include <bits/stdc++.h>
using namespace std;

// ===== Paste your CloudStorage class here =====
class CloudStorage {
public:

    // I am going to start with files unsorted with an unordered_map if later a requirement asks for
    // return the all the files sorted alphabetically I would use map
    //

    struct File {
        // for now we just need size but later we can add other atributes like expiration, or date of creation
        int file_size;
    };
    unordered_map<string, File> files; // {name: File}

    File* findFile(const string& name) {
        auto it = files.find(name);
        if(it == files.end()) return nullptr; // name of file not found in files
        return &it->second;
    }

    bool startsWith(const string& s, const string& prefix) {
        return s.compare(0, prefix.size(), prefix) == 0;
    }

    bool endsWith(const string& s, const string& suffix) {
        return s.size() >= suffix.size()
            && s.compare(s.size() - suffix.size(), suffix.size(), suffix) == 0;
    }

    string format(const string& key, const string& value) {
        return key+"("+value+")";
    }

    // returns the files whose names start with prefix and end with suffix, as "name(size)".
    // Sort by size descending, then by name ascending on ties. Return at most 10 results.
    // An empty prefix or suffix matches everything.
    vector<string> findFiles(int timestamp, const string& prefix, const string& suffix) {
        vector<pair<int, string>> filename_size; // pair<size, name>
        // 1) first we need to filter the filenames that does start with prefix and end with suffix
        for(auto& [name, fl]: files) {
            if(startsWith(name, prefix) && endsWith(name, suffix)){
                filename_size.push_back({fl.file_size, name});
            }
        }
        // 2) sort by size descending then by name ascending
        sort(filename_size.begin(), filename_size.end(), [](const auto& a, const auto&b) {
            if(a.first != b.first) return a.first > b.first; // size descending
            return a.second < b.second; // name ascending
        });
        // 3) get at most 10
        vector<string> result;
        int i=0;
        for(auto& [fl_size, name]: filename_size) {
            if(i>=10) break;
            result.push_back(format(name, to_string(fl_size)));
            i++;
        }
        return result;
    }

    // returns the sum of the sizes of all files whose names start with prefix, or 0 if none match.
    int getTotalSize(int timestamp, const string& prefix) {
        int sum=0;
        for(auto& [name, fl]: files) {
            if(startsWith(name, prefix)){
                sum+=fl.file_size;
            }
        }
        return sum;
    }



    // adds a file with the given size. It returns true if added, or false if a file with that name already exists.
    bool addFile(int timestamp, const string& name, int fl_size) {
        File* fl = findFile(name);
        if(fl != nullptr) return false; // file with this name already exists
        files[name] = File{fl_size};
        return true;
    }

    // returns the file's size, or -1 if it doesn't exist.
    int  getFileSize(int timestamp, const string& name) {
        File* fl = findFile(name);
        if(fl == nullptr) return -1; // file does not exists
        return fl->file_size;
    }

    // deletes the file and returns its size, or -1 if it doesn't exist.
    int  deleteFile(int timestamp, const string& name) {
        File* fl = findFile(name);
        if (fl == nullptr) return -1; // file does not exists can't delete
        int fl_size = fl->file_size;
        files.erase(name);
        return fl_size;
    }

    // creates dest with the same size as source. It returns false (and changes nothing) if source doesn't exist,
    // dest already exists, or they're the same name. The copy is independent: deleting one doesn't affect the other.
    bool copyFile(int timestamp, const string& source, const string& dest) {
        File* src_fl = findFile(source);
        File* dest_fl = findFile(dest);
        if(src_fl == nullptr) return false; // src file does not exists
        if(dest_fl != nullptr) return false; // dest already exists
        if(source.compare(dest) == 0) return false; // src and dest same file
        files[dest] = *src_fl; // copies the whole struct, whatever fields it has
        return true;
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
    CloudStorage cs;
    check("L1 add",              b(cs.addFile(1, "/docs/a.txt", 100)), "true");
    check("L1 add dup",          b(cs.addFile(2, "/docs/a.txt", 50)), "false");
    check("L1 size",             i(cs.getFileSize(3, "/docs/a.txt")), "100");
    check("L1 size missing",     i(cs.getFileSize(4, "/docs/b.txt")), "-1");
    check("L1 add logo",         b(cs.addFile(5, "/img/logo.png", 300)), "true");
    check("L1 copy",             b(cs.copyFile(6, "/docs/a.txt", "/backup/a.txt")), "true");
    check("L1 copy size",        i(cs.getFileSize(7, "/backup/a.txt")), "100");
    check("L1 copy missing src", b(cs.copyFile(8, "/docs/missing.txt", "/x.txt")), "false");
    check("L1 copy dest exists", b(cs.copyFile(9, "/img/logo.png", "/docs/a.txt")), "false");
    check("L1 copy same",        b(cs.copyFile(10, "/docs/a.txt", "/docs/a.txt")), "false");
    check("L1 delete",           i(cs.deleteFile(11, "/docs/a.txt")), "100");
    check("L1 copy independent", i(cs.getFileSize(12, "/backup/a.txt")), "100");
    check("L1 delete again",     i(cs.deleteFile(13, "/docs/a.txt")), "-1");
    check("L1 re-add",           b(cs.addFile(14, "/docs/a.txt", 20)), "true");
    check("L1 new size",         i(cs.getFileSize(15, "/docs/a.txt")), "20");

        // ===== Level 2 (fresh storage) =====
    CloudStorage cs2;
    check("L2 add report",   b(cs2.addFile(1, "/docs/report.txt", 500)), "true");
    check("L2 add notes",    b(cs2.addFile(2, "/docs/notes.txt", 200)), "true");
    check("L2 add draft",    b(cs2.addFile(3, "/docs/draft.txt", 500)), "true");
    check("L2 add image",    b(cs2.addFile(4, "/docs/image.png", 800)), "true");
    check("L2 add photo",    b(cs2.addFile(5, "/img/photo.png", 900)), "true");
    check("L2 add old",      b(cs2.addFile(6, "/docs/archive/old.txt", 50)), "true");
    check("L2 find txt",     v(cs2.findFiles(7, "/docs/", ".txt")),
          "[/docs/draft.txt(500), /docs/report.txt(500), /docs/notes.txt(200), /docs/archive/old.txt(50)]");
    check("L2 find png",     v(cs2.findFiles(8, "/docs/", ".png")), "[/docs/image.png(800)]");
    check("L2 find all png", v(cs2.findFiles(9, "/", ".png")), "[/img/photo.png(900), /docs/image.png(800)]");
    check("L2 find none",    v(cs2.findFiles(10, "/music/", ".mp3")), "[]");
    check("L2 empty suffix", v(cs2.findFiles(11, "/docs/report.txt", "")), "[/docs/report.txt(500)]");
    check("L2 long suffix",  v(cs2.findFiles(12, "/", "/docs/archive/old.txt.bak")), "[]");
    check("L2 total docs",   i(cs2.getTotalSize(13, "/docs/")), "2050");
    check("L2 total sub",    i(cs2.getTotalSize(14, "/docs/archive/")), "50");
    check("L2 total none",   i(cs2.getTotalSize(15, "/nothing/")), "0");
    check("L2 delete notes", i(cs2.deleteFile(16, "/docs/notes.txt")), "200");
    check("L2 total after",  i(cs2.getTotalSize(17, "/docs/")), "1850");

    for (int k = 0; k < 12; k++) cs2.addFile(20 + k, "/many/f" + to_string(k), k);
    auto many = cs2.findFiles(40, "/many/", "");
    check("L2 limit 10",     i(many.size()), "10");
    check("L2 limit first",  many.empty() ? "" : many[0], "/many/f11(11)");
    check("L2 limit last",   many.size() < 10 ? "" : many[9], "/many/f2(2)");

    cout << "\n" << passed << "/" << total << " passed\n";
    return 0;
}
