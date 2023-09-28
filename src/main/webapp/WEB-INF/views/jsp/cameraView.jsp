<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>Face Recognition Payment</title>

    <!-- Bootstrap CSS -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">

    <style>
        #videoElement, #canvasElement {
            max-width: 100%; /* Max width but retaining the aspect ratio */
        }
        #videoElement {
            border-radius: 10px;
        }
    </style>
</head>

<body class="bg-light">

    <div class="container my-5">
        <div class="row justify-content-center">
            <div class="col-md-6">
                <video autoplay="true" id="videoElement" class="w-100 mb-3"></video>
                <canvas id="canvasElement" class="w-100 mb-3"></canvas>
                <div class="d-flex justify-content-between mb-3">
                    <button onclick="startVideo()" class="btn btn-primary">Start</button>
                    <button onclick="stopVideo()" class="btn btn-danger">Stop</button>
                </div>
                <div class="form-group">
                    <label for="username">Enter Username:</label>
                    <input type="text" id="username" name="username" class="form-control">
                </div>
                <button onclick="capture()" class="btn btn-success btn-block mb-3">KYC</button>
                <button onclick="verifyFace()" class="btn btn-info btn-block">Verify Face</button>
            </div>
        </div>
    </div>


<script src="https://code.jquery.com/jquery-3.5.1.slim.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/@popperjs/core@2.9.3/dist/umd/popper.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.min.js"></script>
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
