<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title>Display Webcam Stream</title>

<style>
#container {
	margin: 0px auto;
	width: 500px;
	height: 375px;
	border: 10px #333 solid;
}
#videoElement, #canvasElement {
	max-width: 500px; /* Max width but retaining the aspect ratio */
}
#videoElement {
	background-color: #666;
}
#canvasElement {
    display: none;
    border: 1px solid #000;
}
</style>
</head>

<body>
<div id="container">
	<video autoplay="true" id="videoElement"></video>
    <canvas id="canvasElement"></canvas>
	<button onclick="startVideo()">Start</button>
	<button onclick="stopVideo()">Stop</button>
    <br>
    <br>
    <label for="username">Enter Username:</label>
	<input type="text" id="username" name="username">
	<button onclick="capture()">KYC</button>
	<br>
	<br>
	<button onclick="verifyFace()">Verify Face</button>
</div>

<script>
var video = document.querySelector("#videoElement");
var canvas = document.getElementById("canvasElement");
var ctx = canvas.getContext("2d");

function startVideo() {
  if (navigator.mediaDevices.getUserMedia) {
    navigator.mediaDevices.getUserMedia({ video: true })
      .then(function (stream) {
        video.srcObject = stream;
        video.style.display = "block";  // Show video feed
        canvas.style.display = "none";  // Hide captured image
      })
      .catch(function (error) {
        console.log("Something went wrong when starting the video!");
      });
  }
}

function stopVideo() {
  var stream = video.srcObject;
  var tracks = stream.getTracks();

  for (var i = 0; i < tracks.length; i++) {
    var track = tracks[i];
    track.stop();
  }

  video.srcObject = null;
}

function capture() {
  var username = document.getElementById("username").value;  // Get the username from the input field
  
  // Check if username is empty or null
  if(!username || username.trim() === "") {
      alert("Please enter a username!");
      return;
  }

  canvas.width = video.videoWidth;
  canvas.height = video.videoHeight;
  ctx.drawImage(video, 0, 0, video.videoWidth, video.videoHeight);
  var dataURL = canvas.toDataURL('image/png');
  
  sendDataToServer(dataURL, username);
}


function sendDataToServer(dataURL,username) {
  fetch('/storeImage', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ image: dataURL, username: username })
  })
    .then(response => response.json())  
    .then(data => {
        alert(data.message);  
    })
    .catch(error => {
        console.error("Error:", error);
        alert("Face verification failed!");
    });
}

// Start the webcam stream when the page loads
startVideo();

function verifyFace() {
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    ctx.drawImage(video, 0, 0, video.videoWidth, video.videoHeight);
    var dataURL = canvas.toDataURL('image/png');

    fetch('/checkFace', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ image: dataURL })
    })
    .then(response => response.json())  
    .then(data => {
        alert(data.message);  
    })
    .catch(error => {
        console.error("Error:", error);
        alert("Face verification failed!");
    });
}


</script>
</body>
</html>
