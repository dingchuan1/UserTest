function Action(json) {
    let rel = new Array();
    $.ajax({
        url: 'https://localhost:8080/Login/denglu',
        type: "post",
        data: json,
        success: function (data) {
            rel.push(data);
        },
        error: function (data) {
            rel.push(data);
        }
    })

    return rel;
}