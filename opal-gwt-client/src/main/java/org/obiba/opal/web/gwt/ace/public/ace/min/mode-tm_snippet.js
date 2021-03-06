define("ace/mode/tm_snippet", ["require", "exports", "module", "ace/lib/oop", "ace/mode/text", "ace/tokenizer", "ace/mode/text_highlight_rules"], function (e, t, n) {
    var r = e("../lib/oop"), i = e("./text").Mode, s = e("../tokenizer").Tokenizer, o = e("./text_highlight_rules").TextHighlightRules, u = function () {
        var e = "SELECTION|CURRENT_WORD|SELECTED_TEXT|CURRENT_LINE|LINE_INDEX|LINE_NUMBER|SOFT_TABS|TAB_SIZE|FILENAME|FILEPATH|FULLNAME";
        this.$rules = {start: [
            {token: "constant.language.escape", regex: /\\[\$}`\\]/},
            {token: "keyword", regex: "\\$(?:TM_)?(?:" + e + ")\\b"},
            {token: "variable", regex: "\\$\\w+"},
            {token: function (e, t, n) {
                return n[1] ? n[1]++ : n.unshift("start", 1), this.tokenName
            }, tokenName: "markup.list", regex: "\\${",next:"varDecl"},{token:function(e,t,n){return n[1]?(n[1]--,n[1]||n.splice(0,2),this.tokenName):"text"},tokenName:"markup.list",regex:"}"},
            {token: "doc,comment", regex: /^\${2}-{5,}$/}
        ], varDecl: [
            {regex: /\d+\b/, token: "constant.numeric"},
            {token: "keyword", regex: "(?:TM_)?(?:" + e + ")\\b"},
            {token: "variable", regex: "\\w+"},
            {regex: /:/, token: "punctuation.operator", next: "start"},
            {regex: /\//, token: "string.regex", next: "regexp"},
            {regex: "", next: "start"}
        ], regexp: [
            {regex: /\\./, token: "escape"},
            {regex: /\[/, token: "regex.start", next: "charClass"},
            {regex: "/", token: "string.regex", next: "format"},
            {token: "string.regex", regex: "."}
        ], charClass: [
            {regex: "\\.", token: "escape"},
            {regex: "\\]", token: "regex.end", next: "regexp"},
            {token: "string.regex", regex: "."}
        ], format: [
            {regex: /\\[ulULE]/, token: "keyword"},
            {regex: /\$\d+/, token: "variable"},
            {regex: "/[gim]*:?", token: "string.regex", next: "start"},
            {token: "string", regex: "."}
        ]}
    };
    r.inherits(u, o), t.SnippetHighlightRules = u;
    var a = function () {
        var e = new u;
        this.$tokenizer = new s(e.getRules())
    };
    r.inherits(a, i), function () {
        this.getNextLineIndent = function (e, t, n) {
            return this.$getIndent(t)
        }
    }.call(a.prototype), t.Mode = a
})