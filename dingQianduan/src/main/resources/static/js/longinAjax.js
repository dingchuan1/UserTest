function Action(json) {
    let rel = new Array();
    $.ajax({
        url: 'http://localhost:8080/Login',
        type: "post",
        data: json,
        success: function (data) {
            rel.push(data);
            console.log(data);
        },
        error: function (data) {
            rel.push(data);
        }
    })

    return rel;
}