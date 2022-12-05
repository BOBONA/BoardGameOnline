var client;
var disconnectModalTemplate = `
<div class="modal">
    <div class="modal-content">
        <h4>Disconnected</h4>
        <p>It looks like you disconnected from the server. Reload the page to restore functionality.</p>
    </div>
    <div class="modal-footer">
        <a onclick="reloadPage()" class="waves-effect btn-flat">Reload</a>
    </div>
</div>
`
var disconnectModal;

function setClient(stomp) {
    // initialize modal
    var div = document.createElement("div");
    div.innerHTML = disconnectModalTemplate.trim();
    div = div.firstChild;
    document.body.appendChild(div);
    M.Modal.init(div);
    disconnectModal = div;
    // set methods
    client = stomp;
    client.onStompError = disconnected;
    client.onWebSocketClose = disconnected;
    client.onDisconnect = disconnected;
}

function disconnected() {
    M.Modal.getInstance(disconnectModal).open();
}

function reloadPage() {
    window.location.reload(true);
}