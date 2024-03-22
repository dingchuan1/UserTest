var videoTypes = ["mp4","webm","ogg","ogv","avi","mkv"];

function checkVideoType(filename){

    // 遍历视频格式数组
    for (var i = 0; i < videoTypes.length; i++) {
        // 如果文件名以当前视频格式结尾，则返回true
        if (filename.toLowerCase().endsWith(videoTypes[i])) {
            return true;
        }
    }
    // 如果没有找到匹配的视频格式，则返回false
    return false;
}

function playVideo(filename,filepath){
    let url = "http://localhost:8080/playVideo.html?servicesName=playVideo&videoName="+encodeURIComponent(filename)+"&videoPath="+encodeURIComponent(filepath);
    let videoWindow = window.open(url,'_blank');
    if(videoWindow){
        videoWindow.focus();
    }
}

// 解析查询字符串并返回参数对象
function parseQueryString() {
    const queryString = window.location.search;
    const params = new URLSearchParams(queryString);
    const paramObj = {};
    for (const [key, value] of params.entries()) {
        paramObj[key] = value;
    }
    return paramObj;
}

function onLoad(){
    // 获取查询字符串
    const parameters = parseQueryString();
    if(parameters.videoName != ""){
        let videoUrl = "http://localhost:8080/playVideo?servicesName=playVideo&videoName="+parameters.videoName+"&videoPath="+parameters.videoPath;
        addVideoSource(videoUrl);
    }

}

function addVideoSource(videoUrl){
    let parentElement = document.getElementById('playVideoById');
    let referenceElement = parentElement.childNodes[0]; // 获取参考元素
    for (var i = 0; i < videoTypes.length; i++) {
        let newElement = document.createElement('source');
        newElement.src = videoUrl+""; // 设置src属性
        newElement.type = "video/"+videoTypes[i];
        parentElement.insertBefore(newElement, referenceElement);
    }

}