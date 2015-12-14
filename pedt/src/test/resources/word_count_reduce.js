function word_count_reduce(dicts) {
    print("dict: " + dicts);
    print("dict.size(): " + dicts.size());
    var result = {};
    for(var i = 0; i < dicts.size(); i++) {
        print("dicts[i]: " + JSON.stringify(dicts[i]));
        for(var word in dicts[i]) {
            print(word);
            if(word in result) result[word]+=dicts[i][word];
            else result[word] = dicts[i][word];
        }
    }
    print("result: " + JSON.stringify(result));
    return result;
}
