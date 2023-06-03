let history = [];
let messageBox = document.getElementById("messageBoxSend");
let sendButton = document.getElementById("sendButton");


function sendMessage() {
    let message = messageBox.innerText;
    history.push(message);
    messageBox.innerText = "";
    
}
