//文件上传
//实现逻辑
//1、判断文件大小，大于50MB就实行分片上传
//2、文件分片、md5计算
//3、发起检查请求，把当前文件的hash发送给服务端，检查是否有相同的hash文件
//4、上传进度计算
//5、上传完成后通知后端合并切片
//后端实现
//1、检查接受到的hash是否有相同的文件，并通知前端当前hash是否有未完成的上传
//2、接受切片
//3、合并所有切片

//通常文件切片是将文件切成1MB大小的分块。但是如果是一个10G以上的文件，将会产生1万个以上的切片，由于js是单线程模型
//如果这个切片计算等是在js主线程执行的话，那时候页面都将会被卡死。这时候可以使用【Web Worker】
//Web Worker 的作用，就是为 JavaScript 创造多线程环境，允许主线程创建 Worker 线程，将一些任务分配给后者运行。在主线程运行的同时，Worker 线程在后台运行，两者互不干扰。

//这里使用百度的WebUploader
//这个必须要在uploader实例化前面
WebUploader.Uploader.register({
    'before-send-file':'beforeSendFile',
    'before-send':'beforeSend'
},{
    beforeSendFile:function(file){
        //Deferred()对象在钩子回掉函数中经常要用到，用来处理需要等待的异步操作。
        var task = new $.Deferred();
        //计算文件MD5

        return $.when(task);
    },
    beforeSend:function(){

    }
})

// 实例化WebUploader
var uploader = WebUploader.create({
    //swf文件路径
    swf: '/js/webuploader/Uploader.swf',
    // 选完文件后是否自动上传。
    auto: false,
    // 指定上传的服务器端地址。
    server: 'http://localhost:8080/UpLoadFile?servicesName=upLoadFile',

    // 后端用来接收上传文件的参数名称。
    fileVal: 'multipartFile',

    // 请求超时时间。
    timeout: 60000, // 60 seconds.

    // 指定选择文件的按钮容器。
    pick: '#filePicker',

    //formData:{"tokenValue":""},
    // 是否启用截图功能。默认：false。使用截图会降低性能，建议仅在需要时启用。
    paste: false,

    // 是否自动分片上传。默认：false。若服务器端支持分片上传，建议设置为true。
    chunked: true,

    //设置分片的大小这里为3Mb
    chunkSize: 1024 * 1024 * 3,

    // 是否开启分片上传自动重试功能。默认：false。若服务器端支持分片上传，建议设置为true。在某些网络环境下可以有效降低500错误。
    chunkRetry: true,

    //允许同时最大上传进程数
    threads: 20,
    //允许在文件传输时提前把下一个文件准备好
    prepareNextFile:true
});
//fileQueued,当有文件添加进来的时候
//当文件被添加的时候，就会计算md5值，计算过程是异步的，且文件越大计算越久。
//md5FlagMap用于存储文件md5计算完成的标志位；多个文件时，分别设置标志位，key是文件名，value是true或false
var md5FlagMap = new Map();
uploader.on('fileQueued', function(file) {
    md5FlagMap.set(file.name,false);//文件md5值默认没计算完成。
    //var deferrde = WebUploader.Deferred();//Deferred()用于监控异步计算文件md5值这个异步操作的执行状态。
    // 在文件列表中添加文件信息。
    //var $list = $('#fileList tbody');
    var list = document.getElementById("fileList").getElementsByTagName("tbody");
    var fileId = file.id;
    var funfileClick = "fileClick("+ fileId.toString() +")";
    var lihtml = "<tr id='"+ fileId +"' onclick='"+funfileClick+"' class='file-li'>" +
        "<td data-label='文件名'>" +
            "<div >" +
                "<span style='overflow: hidden;'>"+file.name+"</span>" +
            "</div>" +
        "</td>" +
        "<td data-label='文件信息'>" +
            "<div>" +
                "<span>文件大小：</span>" +
                "<span>"+file.size+"bytes</span>" +
            "</div>" +
            "<div class='text-muted'>" +
                "<span>文件最后修改日：</span>" +
                "<span>"+file.lastModified+"</span>" +
            "</div>" +
        "</td>" +
        "<td>" +
            "<div>" +
                "<span id='readfiletext_"+fileId+"'>等待上传</span>" +
            "</div>" +
            "<div id='progress_"+fileId+"' class='progress progress-li' style='display: none'>" +
                "<div id='progressBar_text_"+fileId+"' class='progress-bar-text-li'>以读取0%</div>" +
                "<div id='progressBar_"+fileId+"' class='progress-bar bg-green progress-bar-li' style='width:0%' role='progressbar' aria-valuenow='0' aria-valuemin='0' aria-valuemax='100'>" +
                "</div>" +
            "</div>" +
        "</td>" +
        "<td>" +
            "<div class='progress progress-li'>" +
                "<div id='progressBar_up_text_"+fileId+"' class='progress-bar-text-li'>等待上传</div>" +
                "<div id='progressBar_up_"+fileId+"' class='progress-bar bg-green progress-bar-li' style='width:0%' role='progressbar' aria-valuenow='0' aria-valuemin='0' aria-valuemax='100'>" +
                "</div>" +
            "</div>" +
        "</td>" +
        "<td>" +
            "<div id='delbtn_"+fileId+"' class='btn btn-purple ' onclick='stopPropag(event); delFileById(this);' >" +
                "撤销" +
            "</div>" +
            "<div id='cancelbtn_"+fileId+"' class='btn btn-purple' style='display: none' onclick='stopPropag(event);cancelUp(this);'>" +
                "取消上传" +
            "</div>"+
        "</td>" +
        "</tr>";
    //var $li = $(lihtml);
    //$list.append($li);
    list[0].innerHTML=lihtml;
    // uploader.md5File(file)
    //     .progress(function(percentage) {
    //         console.log("percentage="+percentage);
    //         $('#progressBar_text_'+ file.id).text('以读取'+percentage+'%');
    //         $('#progressBar_'+ file.id).css('width', percentage + '%');
    //     })
    //     .then(function (fileMd5){
    //         console.log("完成");
    //         file.wholeMd5 = fileMd5;
    //         file_md5 = fileMd5;
    //         //deferrde.resolve(file.name);//文件md5值计算完成后，更新状态为已完成，这时deferred.done()被调用。
    //         md5FlagMap.set(name,true);
    //         $('#progressBar_'+ file.id).css('width', '100%');
    //         $('#progressBar_text_'+ file.id).text('文件读取完成');
    //         $('#progressBar_text_'+ file.id).css('color','white');
    //         $('#readfiletext_'+ file.id).text('文件读取完成');
    //         var timeoutFuns = {
    //             fun1 : {funname:'settimeoutRemoveDot',value1:'progress_'+ file.id},
    //             fun2 : {funname : 'settimeoutChangeText',value1 : 'readfiletext_'+ file.id,value2 : '等待上传'}
    //         }
    //         timeout2fun(timeoutFuns,3000);
    //         $('#'+ file.id +' .btn-loading').removeClass("btn-loading");
    //     })
    //     .catch(function(error) {
    //         console.log("Error occurred:", error);
    //     });
        // deferrde.done(function (name){
        //     md5FlagMap.set(name,true);
        //     $('#progressBar_'+ file.id).css('width', '100%');
        //     $('#progressBar_text_'+ file.id).text('文件读取完成');
        //     $('#progressBar_text_'+ file.id).css('color','white');
        //     $('#readfiletext_'+ file.id).text('文件读取完成');
        //     var timeoutFuns = {
        //         fun1 : {funname:'settimeoutRemoveDot',value1:'progress_'+ file.id},
        //         fun2 : {funname : 'settimeoutChangeText',value1 : 'readfiletext_'+ file.id,value2 : '等待上传'}
        //     }
        //     timeout2fun(timeoutFuns,3000);
        //     $('#'+ file.id +' .btn-loading').removeClass("btn-loading");
        //     var delbtnByfileId = document.getElementById("delbtn_"+file.id);
        //     //$('#delbtn_'+ file.id).on('click',delFileById(file.id+'',delbtnByfileId,true));
        //     delbtnByfileId.addEventListener('click',delFileById(file.id+'',delbtnByfileId,true));
        // });

    //return deferrde.promise();

});
//uploadProgress显示进度条
uploader.on('uploadProgress', function(file, percentage) {
    // 更新进度条。
    $('#uploadProgress').show();
    $('#uploadProgressBar').css('width', percentage + '%');
    $('#uploadProgressBar').find('p').text(percentage+'%');
});

// uploader.addButton({
//     id:"#fileDle"
// })

//uploadBeforeSend，在分片模式下，当文件的分块在发送前触发
uploader.on('uploadBeforeSend',function(block,data){
    var file = block.file;
    //data可以携带参数到后端
    data.name = file.name;//文件名字
    data.md5Value = file.md5;//文件整体的md5值
    data.start = block.start;//分片数据块在整体文件的开始位置
    data.end = block.end;//分片数据块在整体文件的结束位置
    data.chunk = block.chunk;//分片的索引位置
    data.chunks = block.chunks;//整个文件总共分了多少片
});

function fileClick(fileid){
    var delfilelistobj = $("#delfilelist");
    if (delfilelistobj.val().contains(fileid)) {
        if(delfilelistobj.val().contains(","+fileid)){
            delfilelistobj.val(delfilelistobj.val().replace(","+fileid, ""));
        }else{
            delfilelistobj.val(delfilelistobj.val().replace(fileid, ""));
        }
        $("#"+fileid).css('background-color','#86b7fe');
    }else{
        if(delfilelistobj.val() == ""){
            delfilelistobj.val(delfilelistobj.val()+fileid);
        }else {
            delfilelistobj.val(delfilelistobj.val()+","+fileid);
        }
        $("#"+fileid).css("background-color","#ffecb5");
    }

}
/*
    obj:传入this
    isbubble:是否阻止冒泡事件，true阻止，false不阻止
 */
function delFileById(t){
    // 获取元素的ID
    var id = t.id;
    // 截取第一个"_"以后的字符串
    var index = id.indexOf('_');
    if (index !== -1) {
        var fileId = id.slice(index + 1);
    } else {
        console.log("No underscore found.");
    }
    var flieTr = document.getElementById(fileId+"");
    flieTr.parentNode.removeChild(flieTr);
    uploader.removeFile(fileId,true);
}

function stopPropag(e){
    e.stopPropagation();
}

function delSelectFile(){
    var delfilelistobj = $("#delfilelist");
    var elements = delfilelistobj.val().split(',');
    $.each(elements, function(index, value) {
        $("#"+value).remove();
        uploader.removeFile(value,true);
    });
}

function uploadFile() {
    uploader.md5File(file).progress(percentage => {
        console.log("percentage="+percentage);
        $('#progress_'+ file.id).css('display','block');
        $('#readfiletext_'+ file.id).text('正在读取文件');
        $('#progressBar_text_'+ file.id).text('以读取'+percentage+'%');
        $('#progressBar_'+ file.id).css('width', percentage + '%');
    }).then(function (fileMd5){
        console.log("完成");
        file.md5 = fileMd5;

        $('#progressBar_'+ file.id).css('width', '100%');
        $('#progressBar_text_'+ file.id).text('文件读取完成');
        $('#progressBar_text_'+ file.id).css('color','white');
        $('#readfiletext_'+ file.id).text('文件读取完成');
        var timeoutFuns = {
            fun1 : {funname:'settimeoutRemoveDot',value1:'progress_'+ file.id},
            fun2 : {funname : 'settimeoutChangeText',value1 : 'readfiletext_'+ file.id,value2 : '上传中...'},
            fun3 : {funname : 'settimeoutChangeBtn',value1 : 'delbtn_'+ file.id,value2 : 'cancelbtn_'+ file.id}
        }
        timeout2fun(timeoutFuns,3000);
        //这里通过ajax和后台通信根据md5的信息来判断，可实现断点续传
        //服务器应该将传输的分片文件的md5保存，
        // if(retrunstatus == 101){
        //     //分片文件在服务器中不存在，就是正常流程
        // }else if(retrunstatus == 100){
        //     //分片文件在服务器中已存在，标识上传成功并跳过上传过程
        //     uploader.skipFile(file);
        //     file.pass = true;
        // }else if(retrunstatus == 102){
        //     //部分以上传，但是差几个分片
        //     file.missChunks = data.xxxx;
        // }
    });
    //  md5FlagMap里面存储有文件md5计算的状态。
    // 同时上传多个文件时，上传前要判断一下以添加的文件md5计算完成没有。
    //如果有未计算完成的，则继续等待计算结果
    //这个可以优化为，那个文件md5计算完成就上传哪个。
    // var uploadFloag = true;
    // md5FlagMap.forEach(function(value,key){
    //     if(!value){
    //         uploadFloag = false;
    //         alert("文件加载中...");
    //     }
    // });
    // if (uploadFloag){
        uploader.upload();
    // }

}

//为元素添加事件
function addEventById(id,type,funcname,arm){
    var eventById = document.getElementById(id+"");
    var fun = window[funcname];
    var keyCount = arm.length;
    if (typeof fun === 'function') {
        switch (keyCount){
            case 0:
                eventById.addEventListener(type+"",fun());
                break;
            case 1:
                eventById.addEventListener(type+"",fun(arm[0]));
                break;
            case 2:
                eventById.addEventListener(type+"",fun(arm[0],arm[1]));
                break;
            case 3:
                eventById.addEventListener(type+"",fun(arm[0],arm[1],arm[2]));
                break;
            case 4:
                eventById.addEventListener(type+"",fun(arm[0],arm[1],arm[2],arm[3]));
                break;
        }

    }

}

function settimeoutChangeText(id,text){
    $('#'+id).text(''+text);
}

function settimeoutChangeBtn(btn1,btn2){
    $('#'+btn1).css('display','none');
    $('#'+btn2).css('display','block');
}

function settimeoutRemoveDot(id){
    $('#'+id).remove();
}

//这个timeout函数返回一个Promise，这个Promise将在给定的延迟时间后解析，如果函数执行成功，Promise将解析为成功消息。如果在执行函数时抛出错误，Promise将被拒绝并解析为错误消息。
function timeout2fun(funsobj, delay) {
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            try {
                for (var key in funsobj) {
                    if (funsobj.hasOwnProperty(key)) {
                        var funObj = funsobj[key];
                        var keyCount = Object.keys(funObj).length;
                        var fun = window[funObj.funname];
                        if (typeof fun === 'function') {
                            switch (keyCount){
                                case 2:
                                    fun(funObj.value1);
                                    break;
                                case 3:
                                    fun(funObj.value1,funObj.value2);
                                    break;
                            }
                        }


                    }
                }

                resolve("The function executed successfully.");
            } catch (error) {
                reject(new Error(error.message));
            }
        }, delay);
    });
}
/*var uploader = WebUploader.Uploader({
    swf:'./js/webuploader/Uploader.swf',
    dnd:'',//指定Drag And Drop拖拽的容器，如果不指定，则不启动。
    server: 'http://localhost:8080/upload',// 文件接收服务端。
    multiple: true, // 选择多个
    chunked:true,//是否要分片处理大文件上传。
    prepareNextFile:true,//是否允许在文件传输时提前把下一个文件准备好
    chunkSize:5 * 1024 * 1024,//如果要分片，分多大一片？ 默认大小为5M.
    chunkRetry:3,//如果某个分片由于网络问题出错，允许自动重传多少次？
    threads:5,//上传并发数。允许同时最大上传进程数。默认为3
    fileNumLimit:10,//验证文件总数量, 超出则不允许加入队列
    fileSizeLimit:500 * 1024 * 1024 * 1024,//验证文件总大小是否超出限制, 超出则不允许加入队列。500G
    fileSingleSizeLimit:100 * 1024 * 1024 * 1024//验证单个文件大小是否超出限制, 超出则不允许加入队列。100G
});
// 绑定点击选择文件按钮的事件。
document.getElementById('filePicker').addEventListener('click', function() {
    uploader.click();
});

var fileInput = document.getElementById('file-input');
//进度条
var progressBar = document.getElementById('progress-bar-fill');
// 分片大小，这里为2MB
var uploadSize = 2048 * 2048;
// 当前上传的分片索引
var uploadIndex = 0;
// 总分片数
var uploadTotal = 0;
var uploadData;
function uploadFile() {
    var files = fileInput.files;
    if (files.length == 0) {
        alert('请选择文件');
        return;
    }

    uploadData = new Blob(files);
    uploadTotal = Math.ceil(uploadData.size / uploadSize);
    uploadIndex = 0;

    uploadNext();
}

function uploadNext() {
    var start = uploadIndex * uploadSize;
    var end = Math.min(uploadData.size, start + uploadSize);

    var form = new FormData();
    form.append('data', uploadData.slice(start, end));
    form.append('index', uploadIndex);
    form.append('total', uploadTotal);

    var xhr = new XMLHttpRequest();
    xhr.open('POST', 'http://localhost:8080/upload', true);
    xhr.upload.onprogress = function(e) {
        if (e.lengthComputable) {
            var percent = (e.loaded / e.total) * 100;
            progressBar.style.width = percent + '%';
        }
    };
    xhr.onload = function() {
        if (xhr.status == 200) {
            uploadIndex++;
            if (uploadIndex < uploadTotal) {
                uploadNext();
            } else {
                alert('上传完成');
            }
        } else {
            alert('上传失败');
        }
    };
    xhr.send(form);
}*/