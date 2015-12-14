function word_count_map(text) {
    var words = text.split(/\s+/);
    var dict = {};
    for (var i = 0; i < words.length; i++) {
        if(words[i] in dict) dict[words[i]]++;
        else dict[words[i]] = 1;
    }
    return dict;
}
