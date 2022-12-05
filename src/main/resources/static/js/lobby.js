var stompClient;
var csrfHeaderName;
var csrfToken;
var isConnected = false;
var userId;
var username;

var $ = function(id) {
    return document.getElementById(id);
};

window.onload = function() {
    // chat
    setMessageFunction(function(message) {
        stompClient.send("/app/socket/main", {}, `{"type": "CHAT_MESSAGE", "chatMessage": "${message}"}`)
    });
    var data = $("data");
    userId = data.getAttribute("userId");
    username = data.getAttribute("username");
    // connect to server
    var xhttp = new XMLHttpRequest();
    xhttp.open("GET", "/csrf", true);
    xhttp.onreadystatechange = function() {
        if (xhttp.readyState === XMLHttpRequest.DONE) {
            if (xhttp.responseText != "" && !isConnected) {
                var data = JSON.parse(xhttp.responseText);
                csrfHeaderName = data.headerName;
                csrfToken = data.token;
                connect();
            }
        }
    }
    xhttp.send();
}

function connect() {
    var socket = new SockJS("/socket");
    stompClient = StompJs.Stomp.over(socket);
    var headers = {};
    headers[csrfHeaderName] = csrfToken;
    setClient(stompClient);
    stompClient.connect(headers, function(frame) {
        stompClient.subscribe("/socket/main", messageReceived);
        stompClient.subscribe("/socket/main/" + userId, messageReceived);
        stompClient.send("/app/socket/main", {}, '{"type": "GET_GAMES"}');
    });
    isConnected = true;
}

function messageReceived(message) {
    var parsed = JSON.parse(message.body);
    if (parsed.type == "CHAT_MESSAGE") {
        updateChat(parsed.message);
    }
}