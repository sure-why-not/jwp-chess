window.onload = function () {
    getChess();
};

function goRoomList() {
    window.location.href = "/";
}

function getChess() {
    let id = localStorage.getItem("id");
    $.ajax({
        url: "/games/" + id,
        type: 'get',
        success(data) {
            clearPieces();
            let obj = parseToJSON(data);
            if (obj.status === "finished") {
                end();
            } else {
                setGameStatus(obj.status);
                setButton(obj.status);
            }
            setPieces(obj.pieces);
        },
        error(request) {
            let obj = JSON.parse(request.responseText);
            alert(obj.message);
        }
    });
}

function clearPieces() {
    let childNodes = document.getElementsByClassName("board_square");
    $.each(childNodes, function (idx, childNode) {
        childNode.innerHTML = "";
    });
}

function setGameStatus(status) {
    if (status === "playing") document.getElementById("game_status").textContent = "게임 진행 중입니다.";
    if (status === "finished") document.getElementById("game_status").textContent = "게임이 종료됐습니다.";
}

function setButton(status) {
    if (status === "playing") {
        document.getElementById("board_div").className = "selectable";
        document.getElementById("status_btn").hidden = false;
        document.getElementById("end_btn").hidden = false;
        return;
    }
    document.getElementById("board_div").className = "non_selectable";
    document.getElementById("status_btn").hidden = true;
    document.getElementById("end_btn").hidden = true;
}

function setPieces(pieces) {
    $.each(pieces, function (idx, piece) {
        setPiece(piece.position, piece.color, piece.type);
    });
}

function setPiece(position, color, type) {
    document.getElementById(position).innerHTML = '<img src=/images/' + color + '_' + type + '.png' + ' class=piece alt="">';
}

let positions = []

function move(position) {
    positions.push(position);
    if (positions.length < 2) {
        return;
    }

    let id = localStorage.getItem("id");
    let sourcePosition = positions[0];
    let targetPosition = positions[1];
    $.ajax({
        url: "/games/" + id + "/pieces",
        type: 'put',
        traditional: true,
        beforeSend: function (xhr) {
            xhr.setRequestHeader("Content-type", "application/json");
        },
        data: JSON.stringify({
            source: sourcePosition,
            target: targetPosition
        }),
        success() {
            document.getElementById(targetPosition).innerHTML = "";
            document.getElementById(sourcePosition).innerHTML = "";
            getChess();
        },
        error(request) {
            let obj = JSON.parse(request.responseText);
            alert(obj.message);
        }
    });
    positions = []
}

function showScore() {
    let id = localStorage.getItem("id");

    $.ajax({
        url: "/games/" + id + "/score",
        type: 'get',
        success(data) {
            let score = parseToJSON(data);
            let message = "";
            $.each(score.scores, function (idx, score) {
                message += score.name + " : " + score.score + "점\n";
            });
            if (score.winnerName === "") {
                message += "동점입니다.";
            } else {
                message += score.winnerName + " 진영이 이기고 있습니다.";
            }
            alert(message);
        },
        error(request) {
            let obj = JSON.parse(request.responseText);
            alert(obj.message);
        }
    });
}

function end() {
    let id = localStorage.getItem("id");

    $.ajax({
        url: "/games/" + id,
        type: 'put',
        success(data) {
            let status = parseToJSON(data);
            var message = "게임 종료 !!!\n♟ 게임 결과 ♟\n";
            $.each(status.scores, function (idx, score) {
                message += score.name + " : " + score.score + "점\n";
            });
            if (status.winnerName === "") {
                message += "동점입니다.";
            } else {
                message += status.winnerName + " 진영이 이겼습니다.";
            }
            alert(message);
            setGameStatus("finished");
            setButton("finished");
        },
        error(request) {
            let obj = JSON.parse(request.responseText);
            alert(obj.message);
        }
    });
}

function parseToJSON(data) {
    if (typeof data == "string") {
        data = JSON.parse(data);
    }
    return data;
}