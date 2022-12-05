var stompClient;
var isConnected = false;
var csrfHeaderName;
var csrfToken;
var gameId = window.location.pathname.split("/")[2];
var userData;
var userId;
var role;
var teamId;
var username;
var canvas;
var ctx;
var borderColor = 1;
var gameData;
var connectedPlayers;
var disconnectedPlayers;
var teamRenderData;
var renderData;
var tileData;
var tileReflectX;
var tileReflectY;
var scoreboardData;
var scoreboardReflectX;
var scoreboardReflectY;
var move = [];
var previousMove = [];
var opponentsMove = [];
var refreshIntervalId;
var incompleteMove = false;
var waitingForResponse = false;
var images = new Map();

function pageLoaded() {
    setMessageFunction(function (chatMessage) {
        if (role != "SPECTATOR") {
            sendStompMessage(`{"type": "CHAT", "chatMessage": "${chatMessage}"}`);
        }
    });
    userData = document.getElementById("user-data");
    userId = userData.getAttribute("userid");
    role = "SPECTATOR";
    teamId = -1;
    username = userData.getAttribute("username");
    canvas = document.getElementById("gameCanvas");
    ctx = canvas.getContext("2d");
    canvas.onclick = canvasClicked;
    window.setInterval(function () {
        if (gameData != undefined && gameData.canMove == true) {
            borderColor = 1 - borderColor;
            if (borderColor == 1) {
                canvas.style.border = "2px solid #000000";
            } else if (borderColor == 0) {
                canvas.style.border = "2px solid #FFFFFF";
            }
        } else {
            borderColor = 1;
            canvas.style.border = "2px solid #000000";
        }
    }, 750);
    updateCanvas();
    disconnect();
    getResource("/csrf", function(response) {
        loadCsrfToken(response);
        connect();
    });
}

function updateUi() {
    var newGameButton = document.getElementById("newgame");
    if (role === "CREATOR" && gameData != null && gameData.winner != -1) {
        newGameButton.style.display = "";
    } else {
        newGameButton.style.display = "none";
    }
    var startButton = document.getElementById("start");
    if (role === "CREATOR") {
        startButton.style.display = "";
        if (!gameData.gameStarted && (connectedPlayers.length + disconnectedPlayers.length) == gameData.maxPlayers) {
            startButton.classList.remove("disabled");
        } else {
            startButton.classList.add("disabled");
        }
    } else {
        startButton.style.display = "none";
    }
    var leaveButton = document.getElementById("leave");
    if (!gameData.gameStarted) {
        leaveButton.classList.remove("disabled");
    } else {
        leaveButton.classList.add("disabled");
    }
    var chatInput = document.getElementById("chatinput");
    if (role === "SPECTATOR" && userData.getAttribute("spectatorchat") === "false") {
        chatInput.disabled = true;
    } else {
        chatInput.disabled = false;
    }
}

function updateCanvas() {
    if (renderData == undefined) {
        var container = document.getElementById("gamecontainer");
        if (container.offsetWidth >= container.offsetHeight) {
            canvas.height = container.offsetHeight;
            canvas.width = canvas.height;
        } else {
            canvas.width = container.offsetWidth;
            canvas.height = canvas.width;
        }
        blurGame("Loading...")
        if (gameData != undefined) {
            loadRenderData();
        }
        return;
    } else {
        canvas.height = document.getElementById("main").offsetHeight * 0.9;
        canvas.width = canvas.height / (renderData.height / renderData.width);
        if (teamRenderData == undefined) {
            updateRenderVariables();
        }
    }
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    drawImage(teamRenderData.background, 0, 0, canvas.width, canvas.height);
    tileData.forEach(function (tile) {
        var index = tile.id;
        renderData.tileImages.forEach(function (tileRange) {
            var range = tileRange.idRange.split("-");
            if (index >= range[0] && index <= range[1]) {
                var tileX = transformXWithWidth(tile.x, tile.width, tileReflectX);
                var tileY = transformYWithHeight(tile.y, tile.height, tileReflectY);
                var tileWidth = canvas.width * tile.width / renderData.width;
                var tileHeight = canvas.height * tile.height / renderData.height;
                drawImage(
                    tileRange.images[gameData.tiles[index]],
                    tileX, tileY, tileWidth, tileHeight
                );
                if ((gameData.continuations != undefined && gameData.continuations.includes(index)) || move.includes(index)) {
                    drawImage(
                        "/images/general/highlight.png",
                        tileX, tileY, tileWidth, tileHeight
                    );
                }
                if (opponentsMove.includes(index)) {
                    drawImage(
                        "/images/general/fainterHighlight.png",
                        tileX, tileY, tileWidth, tileHeight
                    );
                }
            }
        });
    });
    if (gameData.gameStarted) {
        scoreboardData.forEach(function (scoreboard) {
            var text = "";
            if (scoreboard.type == "timer") {
                var timerStart = gameData.scoreboards[scoreboard.linkedId]; // last recorded value of timer from database
                if (gameData.winner == -1) {
                    timerStart += (new Date()).getTime() - gameData.scoreboards[scoreboard.lastTimestampId]; // add newly calculated difference
                }
                // need to deal with timezones and stuff eventually, probably just ask the server for its timezone and calculate proper time from there?
                var roundedTime = Math.floor(timerStart / scoreboard.round) * 1000 / scoreboard.display;
                text = roundedTime.toString();
            } else if (scoreboard.type == "text") {
                text = gameData.scoreboards[scoreboard.linkedId].toString();
            } else if (scoreboard.type == "switchText") {
                text = scoreboard.textOptions[gameData.scoreboards[scoreboard.linkedId]];
            }
            var locX = transformX(scoreboard.x, false)
            var locY = transformY(scoreboard.y, false);
            if (scoreboardReflectX) {
                locX = transformX(scoreboard.x, true) + ctx.measureText(text).width;
            }
            if (scoreboardReflectY) {
                locY = transformYWithHeight(scoreboard.y, scoreboard.fontSize, true);
            }
            if (text.length != 0) {
                drawText(text, locX, locY, true, false, scoreboard.fontSize, false, scoreboard.maxWidth || 1000000);
            }
        });
    }
    if (!gameData.gameStarted) {
        blurGame("Waiting: " + (connectedPlayers.length + disconnectedPlayers.length) + "/" + gameData.maxPlayers + " players")
    } else if (gameData.winner != -1) {
        if (gameData.winner == 0) {
            blurGame("It's a draw!");
        } else {
            var winners = [];
            connectedPlayers.concat(disconnectedPlayers).forEach(function (player) {
                if (player.teamId == gameData.winner) {
                    winners.push(player.name);
                }
            });
            // this is completely necessary for the sake of grammar
            text = "";
            if (winners.length == 0) {
                text = "You lose!";
            } else if (winners.length == 1) {
                text = `${winners[0]} wins!`;
            } else if (winners.length == 2) {
                text = `${winners[0]} and ${winners[1]} win!`
            } else {
                text = winners.subarray(0, winners.length - 1).join(", ") + ", and " + winners[winners.length - 1];
            }
            blurGame(text);
        }
    }
}

function transformXWithWidth(x, width, reflectX) {
    var transformed = transformX(x, reflectX);
    if (reflectX) {
        transformed -= width * canvas.width / renderData.width;
    }
    return transformed;
}

function transformX(x, reflectX) {
    if (reflectX) {
        return canvas.width * (renderData.width - x + renderData.reflectTransformX || 0) / renderData.width;
    } else {
        return canvas.width * (x + renderData.transformX) / renderData.width;
    }
}

function transformYWithHeight(y, height, reflectY) {
    var transformed = transformY(y, reflectY);
    if (!reflectY) {
        transformed -= height * canvas.height / renderData.height;
    }
    return transformed;
}

function transformY(y, reflectY) {
    if (reflectY) {
        return canvas.height * (y + renderData.reflectTransformY || 0) / renderData.height;
    } else {
        return canvas.height * (renderData.height - y - renderData.transformY) / renderData.height;
    }
}

function canvasClicked(event) {
    if (role == "SPECTATOR" || !gameData.canMove) {
        return;
    }
    var rect = canvas.getBoundingClientRect();
    var x = event.clientX - rect.left;
    var y = event.clientY - rect.top;
    // untransform the points back to virtual coordinates
    if (tileReflectX) {
        x = canvas.width - x;
    }
    if (tileReflectY) {
        y = canvas.height - y;
    }
    x = x * renderData.width / canvas.width - renderData.transformX;
    y = -y * renderData.height / canvas.height + renderData.height - renderData.transformY;
    if (tileReflectX) {
        x += renderData.transformX + renderData.reflectTransformX;
    }
    if (tileReflectY) {
        y += renderData.transformY + renderData.reflectTransformY;
    }
    var tileId = null;
    tileData.forEach(function (tile) {
        if (x > tile.x && x < tile.x + tile.width && y > tile.y && y < tile.y + tile.height) {
            tileId = tile.id;
        }
    });
    console.log(tileId);
    if (tileId != null && !waitingForResponse) {
        move.push(tileId);
        if (renderData.requestContinuations) {
            sendData("CONTINUATION_PREVIEW");
        } else {
            sendData("VALIDATE");
        }
        waitingForResponse = true;
    }
}

function blurGame(message) {
    ctx.drawImage(document.getElementById("blur"), 0, 0, canvas.width, canvas.height);
    drawText(message, canvas.width / 2, canvas.height / 2, true, true, 60, true, canvas.width * 0.75);
}

function drawText(text, x, y, centerX, centerY, fontSize, absoluteSize, maxWidth) {
    if (centerX) {
        ctx.textAlign = "center";
    } else {
        ctx.textAlign = "start";
    }
    if (renderData == undefined || absoluteSize) {
        ctx.font = fontSize + "px Arial";
    } else {
        ctx.font = canvas.height / renderData.height * fontSize + "px Arial";
    }
    var verticalSpacing = 5;
    if (!absoluteSize) {
        maxWidth = canvas.width / renderData.width * maxWidth;
    }
    var lines = getLines(text, maxWidth);
    var totalHeight = lines.length * getFontHeight() + (lines.length - 1) * verticalSpacing;
    var yPos = y;
    if (centerY) {
        yPos += totalHeight / 2;
    }
    lines.forEach(function (line) {
        ctx.fillText(line, x, yPos);
        yPos += getFontHeight() + verticalSpacing;
    });
}

function getFontHeight() {
    return ctx.measureText("M").width; // Assuming M is a square
}

function getLines(text, maxWidth) {
    var words = text.split(" ");
    var lines = [];
    var currentLine = words[0];
    for (var i = 1; i < words.length; i++) {
        var word = words[i];
        var width = ctx.measureText(currentLine + " " + word).width;
        if (width < maxWidth) {
            currentLine += " " + word;
        } else {
            lines.push(currentLine);
            currentLine = word;
        }
    }
    lines.push(currentLine);
    return lines;
}

function drawImage(image, x, y, w, h) {
    if (!images.has(image)) {
        loadGameImage(image);
    } else if (images.get(image) != undefined) {
        ctx.drawImage(images.get(image), x, y, w, h);
    }
}

function connect() {
    var socket = new SockJS("/socket");
    stompClient = StompJs.Stomp.over(socket);
    stompClient.onDisconnect = disconnect;
    var headers = {};
    headers[csrfHeaderName] = csrfToken;
    setClient(stompClient);
    stompClient.connect(headers, function(frame) {
        stompClient.subscribe("/socket/game/" + gameId, stompMessageReceived);
        stompClient.subscribe("/socket/game/" + gameId + "/" + userId, stompMessageReceived);
        requestUpdate();
    });
    isConnected = true;
}

function stompMessageReceived(messageOutput) {
    var parsedOutput = JSON.parse(messageOutput.body);
    var submitButton = document.getElementById("submit");
    submitButton.classList.add("disabled");
    waitingForResponse = false;
    if (parsedOutput.responseType == "UPDATE") {
        gameData = parsedOutput;
        if (!gameData.gameStarted) {
            resetLocalVariables();
        }
        if (renderData != undefined) {
            updateRenderVariables();
        }
        sendStompMessage('{"type": "FETCH_PLAYERS"}');
        updateUi();
        updateCanvas();
    } else if (parsedOutput.responseType == "VALIDATE") {
        if (parsedOutput.moveIsValid == 1) {
            sendData("MOVE");
        } else {
            error();
        }
        waitingForResponse = false;
        move = [];
    } else if (parsedOutput.responseType == "CONTINUATION_PREVIEW") { // this functionality might be too checkers specific, will know eventually
        gameData.tiles = parsedOutput.tiles;
        if (parsedOutput.moveIsValid == 0) { // trust past BOBONA that the logic checks out
            var alreadySelected = move.slice(0, move.length - 1).includes(move[move.length - 1]);
            if (incompleteMove && move.length - 1 == 1 && !alreadySelected) { // recalculate new move without error if the player has only done part of the move
                move = [move[move.length - 1]];
                continuations = [];
                sendData("CONTINUATION_PREVIEW");
                waitingForResponse = true;
            } else if (alreadySelected) { // unselect tile from move if already selected
                var temp = [];
                move.forEach(function (movePart) {
                    if (movePart != move[move.length - 1]) {
                        temp.push(movePart);
                    }
                });
                move = temp;
                gameData.continuations = [];
            } else {
                move = move.slice(0, move.length - 1); // remove error part of move
                if (move.length == 0) {
                    move = [...previousMove];
                }
                error();
            }
        } else {
            gameData.continuations = parsedOutput.continuations;
        }
        if (parsedOutput.moveIsValid == 1) {
            incompleteMove = false;
            if (parsedOutput.continuations.length == 0) {
                sendData("MOVE");
                waitingForResponse = true;
                move = [];
            } else {
                submitButton.classList.remove("disabled");
                // MAYBE: unrelated to valid moves, the user needs a way to reset their move (aka requestUpdate)
            }
        }
        if (parsedOutput.moveIsValid == 2) {
            previousMove = [...move];
            incompleteMove = true;
        }
        updateCanvas();
    } else if (parsedOutput.responseType == "NEED_UPDATE") {
        requestUpdate();
        waitingForResponse = true;
    } else if (parsedOutput.responseType == "MOVE") {
        requestUpdate();
        opponentsMove = parsedOutput.lastMove.squares;
        move = [];
        previousMove = [];
        incompleteMove = false;
    } else if (parsedOutput.responseType == "CHAT") {
        updateChat(parsedOutput.message);
    } else if (parsedOutput.responseType == "FETCH_PLAYERS") {
        connectedPlayers = parsedOutput.connectedPlayers;
        disconnectedPlayers = parsedOutput.disconnectedPlayers;
        document.getElementById("online").textContent = "Online: " + joinPlayerNames(parsedOutput.connectedPlayers);
        document.getElementById("offline").textContent = "Offline: " + joinPlayerNames(parsedOutput.disconnectedPlayers);
        if (gameData != null) {
            updateUi();
        }
        updateCanvas();
    } else if (parsedOutput.responseType == "REQUEST_UPDATE") {
        requestUpdate();
    } else if (parsedOutput.responseType == "STATUS_CHAT") {
        var chat = document.getElementById("chat");
        if (chat.textContent.length != 0) {
            chat.innerHTML += "<br>";
        }
        var bold = document.createElement("b");
        var text = document.createTextNode(parsedOutput.message.text);
        bold.appendChild(text);
        chat.appendChild(bold);
        chat.scrollTop = chat.scrollHeight;
    } else if (parsedOutput.responseType == "ROLE_DATA") {
        if (parsedOutput.team != null) {
            teamId = parsedOutput.team;
        }
        if (parsedOutput.role != null) {
            role = parsedOutput.role;
        }
        updateUi();
        updateCanvas();
    }
}

function joinPlayerNames(players) {
    var text = "";
    players.forEach(function (player) {
        text += player.name + ", ";
    });
    text = text.substring(0, text.length - 2);
    return text;
}

function sendData(requestType) {
    sendStompMessage(`{"type": "${requestType}", "move": {"squares": ["${move.join('", "')}"]}}`);
}

function error() {
    window.setTimeout(function() {
        ctx.globalAlpha = 0.2;
        ctx.fillStyle = "#ff5c57";
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.globalAlpha = 1.0;
        window.setTimeout(function () {
            updateCanvas();
        }, 150);
    }, 10);
}

function startGame() {
    sendStompMessage('{"type": "START"}');
}

function requestUpdate() {
    sendStompMessage('{"type": "UPDATE"}');
}

function sendStompMessage(message) {
    stompClient.send("/app/socket/game/" + gameId, {}, message);
}

function disconnect() {
    if (stompClient != undefined) {
        stompClient.disconnect();
    }
    isConnected = false;
}

function getResource(resource, processResponse) {
    var xhttp = new XMLHttpRequest();
    xhttp.open("GET", resource, true);
    xhttp.onreadystatechange = function() {
        if (xhttp.readyState === XMLHttpRequest.DONE) {
            processResponse(xhttp.responseText);
        }
    }
    xhttp.send();
}

function loadCsrfToken(response) {
    if (response != "" && !isConnected) {
        var data = JSON.parse(response);
        csrfHeaderName = data.headerName;
        csrfToken = data.token;
    }
}

function loadRenderData() {
    getResource("/gameLayouts/gameIndex.json", function(response) {
        getResource("/gameLayouts/" + JSON.parse(response)[gameData.gameType], function(response) {
            renderData = JSON.parse(response);
            if (renderData.refreshInterval != undefined) {
                if (refreshIntervalId != undefined) {
                    window.clearInterval(refreshIntervalId);
                }
                refreshIntervalId = window.setInterval(function () {
                    updateCanvas();
                }, renderData.refreshInterval);
            }
            updateCanvas();
        });
    });
}

function updateRenderVariables() {
    if (teamId != -1) {
        teamRenderData = renderData.data.filter(data => data.teamId == teamId || data.teamId == 0)[0];
    } else {
        teamRenderData = renderData.data[0];
    }
    if (teamRenderData.tiles.source != undefined) {
        tileData = renderData.data.filter(data => data.teamId == teamRenderData.tiles.source)[0].tiles;
        tileReflectX = teamRenderData.tiles.reflectX;
        tileReflectY = teamRenderData.tiles.reflectY;
    } else {
        tileData = teamRenderData.tiles;
        tileReflectX = false;
        tileReflectY = false;
    }
    if (teamRenderData.scoreboards.source != undefined) {
        scoreboardData = renderData.data.filter(data => data.teamId == teamRenderData.scoreboards.source)[0].scoreboards;
        scoreboardReflectX = teamRenderData.scoreboards.reflectX;
        scoreboardReflectY = teamRenderData.scoreboards.reflectY;
    } else {
        scoreboardData = teamRenderData.scoreboards;
        scoreboardReflectX = false;
        scoreboardReflectY = false;
    }
}

function loadGameImage(pathname) {
    var image = new Image();
    console.log("loading: " + pathname);
    images.set(pathname, undefined);
    image.onload = function() {
        images.set(pathname, image);
        updateCanvas();
    };
    image.src = pathname;
}

function submit() {
    sendData("MOVE");
}

function leave() {
    if (role != "SPECTATOR") {
        sendStompMessage('{"type": "LEAVE"}');
    }
    window.location.href = "/games";
}

function newGame() {
    sendStompMessage('{"type": "NEW_GAME"}');
}

function resetLocalVariables() {
    move = [];
    previousMove = [];
    opponentsMove = [];
    refreshIntervalId;
    incompleteMove = false;
}