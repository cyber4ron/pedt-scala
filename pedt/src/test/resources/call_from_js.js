function xx() {
    var f1 = pedt_scala.run("script:javascript:base64:ZnVuY3Rpb24gdGVzdCgpIHsKICAgIHByaW50KCJvay4iKTsKICAgIHJldHVybiAib2suIjsKfQo=",
        {});
    var res1 = pedt_scala.wait_within(f1, 1000);
    print("res1: " + res1);

    var f12 = pedt_scala.run(function xxx(p1, p2){ print("p1: " + p1); print("p2: " + p2); return p2; },
        {p1: "x", p2: ["x", "y"]});
    var res12 = pedt_scala.wait_within(f12, 10000);
    print("res12: " + res12);

    var f3 = pedt_scala.reduce("n4c:/a/b/c/map:*", "893d7569b4c01d8c8b6d3e053ebafb66",
        {p1: "x", p2: ["x", "y"]},
        function xxx(x){ print("x: "+ x) ;return x; });
    var res3 = pedt_scala.wait_within(f3, 10000);
    print("res3: " + res3);

    return res3;
}
