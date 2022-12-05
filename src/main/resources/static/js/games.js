var stompClient;
var csrfHeaderName;
var csrfToken;
var isConnected = false;
var userId;
var username;
var gameTemplate = `
<div class="gameinfo z-depth-1">
    <img class="icon"></img>
    <div class="gamename">
       <p class="name"></p>
       <p class="creator"></p>
    </div>
    <div class="separator"></div>
    <p class="status"></p>
    <p class="playerCount"></p>
    <div onClick="joinGame(this.parentNode.id)" class="joinbtn waves-effect waves-teal btn">
       <i class="lockicon material-icons right">lock</i>
       <span class="joinbtntext"></span>
    </div>
</div>
`;
var modals;
var visibleGames = 0;

var $ = function(id) {
    return document.getElementById(id);
};

window.onload = function() {
    // materialize initialization
    var selectList = document.querySelectorAll('select');
    var selects = M.FormSelect.init(selectList);
    var modalList = document.querySelectorAll('.modal');
    modals = M.Modal.init(modalList);
    window.onresize();
    var data = $("data");
    userId = data.getAttribute("userId");
    username = data.getAttribute("username");
    $("id").value = data.getAttribute("joinGameId");
    if (data.getAttribute("joinForm")) {
        modals.filter(modal => modal.id === "joinModal")[0].open();
    }
    if (data.getAttribute("spectatePrompt")) {
        modals.filter(modal => modal.id === "spectateModal")[0].open();
    }
    if (data.getAttribute("newGamePrompt")) {
        newGameClicked();
        modals.filter(modal => modal.id === "newGameModal")[0].open();
    }
    // game filters
    $("typeFilter").onchange = updateGames;
    $("searchFilter").onkeyup = updateGames;
    $("startedFilter").onchange = updateGames;
    $("securedFilter").onchange = updateGames;
    $("joinedFilter").onchange = function () {
        $("startedFilter").checked = $("joinedFilter").checked;
        updateGames();
    }
    clearSelection();
    $("secureGame").onchange = function () {
        if ($("secureGame").checked) {
            $("gamePassword").disabled = false;
        } else {
            $("gamePassword").disabled = true;
            $("gamePassword").value = "";
        }
    };
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

window.onresize = function() {
    Array.from(document.getElementsByClassName("icon")).forEach(function (icon) {
        icon.style.width = document.getElementsByClassName("gamename")[0].offsetHeight + "px";
        icon.style.height = document.getElementsByClassName("gamename")[0].offsetHeight + "px";
    });
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
    if (parsed.type == "GET_GAMES") {
        loadGames(parsed.allGames.reverse());
        updateGames();
    } else if (parsed.type == "GAME_ADDED") {
        var div = createGameDiv(parsed.game);
        var games = $("games");
        games.insertBefore(div, games.firstChild);
        updateGameVisibility($(parsed.game.gameId + ""));
        if (visibleGames > 0) {
            $("nogames").style.display = "none";
        }
        window.onresize();
    } else if (parsed.type == "GAME_UPDATED") {
        updateGameDiv($("" + parsed.game.gameId), parsed.game);
        updateGameVisibility($(game.gameId + ""));
    } else if (parsed.type == "GAME_REMOVED") {
        $("" + parsed.game.gameId).remove();
        if (visibleGames <= 0) {
            $("nogames").style.display = "";
        }
    }
}

function loadGames(games) {
    var gameDiv = $("games");
    Array.from(gameDiv.querySelector(".gameinfo") ?? []).forEach(function (info) {
        info.destroy();
    });
    games.forEach(function (game) {
        var div = createGameDiv(game);
        gameDiv.appendChild(div);
    });
    window.onresize();
}

function createGameDiv(game) {
    var div = document.createElement('div');
    div.innerHTML = gameTemplate.trim();
    div = div.firstChild;
    updateGameDiv(div, game);
    return div;
}

function updateGameDiv(div, data) {
    div.id = data.gameId;
    div.querySelector(".icon").src = ["/images/icons/TTT_icon.png", "/images/icons/Checkers_icon.png", "/images/icons/Connect4_icon.png"][data.gameType]
    div.setAttribute("gameType", data.gameType);
    div.querySelector(".name").textContent = data.gameName;
    div.querySelector(".creator").textContent = data.creatorName;
    div.querySelector(".status").textContent = !data.started ? 'Waiting' : (!data.ended ? 'Started' : 'Ended');
    if (data.started) {
        div.setAttribute("started", true);
    }
    div.querySelector(".playerCount").textContent = data.players + '/' + data.maxPlayers;
    if (data.started && !data.canSpectate && !data.joined) {
        div.querySelector(".joinbtn").classList.add("disabled");
    } else if (data.canSpectate) {
        div.setAttribute("spectate", true);
    }
    if (data.started || !data.secured) {
        div.querySelector(".lockicon").style.display = "none";
    } else {
        div.setAttribute("secured", true);
    }
    var text = "";
    if (data.joined) {
        text = "Reconnect";
        div.setAttribute("joined", true);
    } else if (!data.started) {
        text = "Join";
    } else {
        text = "Spectate";
    }
    div.querySelector(".joinbtntext").textContent = text;
}

function joinBtnClicked() {
    $("joinForm").submit();
}

function joinGame(id) {
    $("id").value = id;
    var infoDiv = $(id + "");
    if (infoDiv.hasAttribute("joined")) {
        spectateGame();
    } else if (infoDiv.hasAttribute("secured")) {
        modals.filter(modal => modal.id === "passwordModal")[0].open();
    } else if (infoDiv.hasAttribute("spectate") && infoDiv.hasAttribute("started")) {
        spectateGame();
    } else {
        joinBtnClicked();
    }
}

function passwordEntered() {
    $("password").value = $("passwordPromptInput").value;
    joinBtnClicked();
}

function spectateGame() {
    window.location.replace("/game/" + $("id").value)
}

function clearSelection() {
    $("searchFilter").value = "";
    $("typeFilter").selectedIndex = "0";
    M.FormSelect.init($("typeFilter"));
    $("startedFilter").checked = false;
    $("securedFilter").checked = true;
    $("joinedFilter").checked = false;
    updateGames();
}

function updateGameVisibility(gameDiv) {
    // pls don't judge me
    if ((!$("startedFilter").checked && gameDiv.hasAttribute("started")) ||
        (!$("securedFilter").checked && gameDiv.hasAttribute("secured")) ||
        ($("typeFilter").value != "" && gameDiv.getAttribute("gameType") != $("typeFilter").value) ||
        ($("joinedFilter").checked && !gameDiv.hasAttribute("joined")) ||
        ($("searchFilter").value != "" &&
        !gameDiv.querySelector(".name").innerHTML.includes($("searchFilter").value) &&
        !gameDiv.querySelector(".creator").innerHTML.includes($("searchFilter").value))) {
        if (gameDiv.style.display != "none") {
            visibleGames--;
            if (visibleGames < 0) {
                visibleGames = 0;
            }
        }
        gameDiv.style.display = "none";
        return false;
    } else {
        gameDiv.style.display = "flex";
        visibleGames++;
        return true;
    }
}

function updateGames() {
    var anyVisible = false;
    Array.from(document.getElementsByClassName("gameinfo")).forEach(function (gameDiv) {
        var visible = updateGameVisibility(gameDiv);
        anyVisible = anyVisible || visible;
    });
    if (anyVisible) {
        $("nogames").style.display = "none";
    } else {
        $("nogames").style.display = "";
    }
    window.onresize();
}

function newGameClicked() {
    $("name").value = username + "'s Game";
    $("expireAfter").value = 1;
    Array.from(document.getElementsByClassName("checkBox")).forEach(function (checkBox) {
        var hidden = checkBox.querySelectorAll("input[type='hidden']")[0];
        hidden.remove();
        checkBox.insertBefore(hidden, checkBox.firstChild);
    });
    M.updateTextFields();
}