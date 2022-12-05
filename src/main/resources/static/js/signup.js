window.onload = function () {
    document.getElementById("name").onkeyup = nameIsAvailable;
    document.getElementById("name").onchange();
    document.getElementById("email").onchange();
}

function nameIsAvailable() {
    var xhttp = new XMLHttpRequest();
    var name = document.getElementById("name");
    if (name.value.length == 0) {
        return;
    }
    xhttp.open("GET", "/userExists/" + name.value, true);
    xhttp.onreadystatechange = function() {
        if (xhttp.readyState === XMLHttpRequest.DONE) {
            if (xhttp.response === "true") {
                name.setCustomValidity("Username not available");
            } else {
                name.setCustomValidity("");
            }
        }
    }
    xhttp.send();
}
