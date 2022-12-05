var sendMessageFunction;

function setMessageFunction(messageFunction) {
    sendMessageFunction = messageFunction;
}

function updateChat(message) {
    var chat = document.getElementById("chat");
    if (chat.textContent.length != 0) {
        chat.innerHTML += "<br>";
    }
    var text = document.createTextNode(`${message.username}: ${message.text}`);
    chat.appendChild(text);
    var container = document.getElementById("chatcontainer");
    chatcontainer.scrollTop = chatcontainer.scrollHeight;
}

function sendMessage() {
    var chatinput = document.getElementById("chatinput");
    if (chatinput.value.length != 0) {
        sendMessageFunction(chatinput.value);
        chatinput.value = "";
        keyReleased();
    }
}

function keyPressed(event) {
    if (event.key === "Enter") {
        sendMessage();
    }
}

function keyReleased() {
    var chatButton = document.getElementById("chatbutton");
    var chatInput = document.getElementById("chatinput");
    if (chatInput.value === "" || chatInput.classList.contains("disabled")) {
        chatButton.classList.add("disabled");
    } else {
        chatButton.classList.remove("disabled");
    }
}