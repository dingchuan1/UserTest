function Action(type,url,json){
    let rel = new Array();
    $.ajax({
        url:url,
        type:type,
        data:json,
        success:function(data){
            rel.push(data);
        },
        error:function(data){
            rel.push(data);
        }
    })

    return rel;
}