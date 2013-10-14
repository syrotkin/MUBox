
// Tutorial from: http://www.sitepoint.com/html5-ajax-file-upload/
var fileDragUpload =   {

/*******************Helper functions***************/
		// shortcut to document.getElementById a la jQuery
		$id:  function(id) {
			return document.getElementById(id);
		},

		outputMessage: function(message) {
			var messagesDiv = fileDragUpload.$id("messages");
			messagesDiv.innerHTML = message + messagesDiv.innerHTML;
		},


/*****************Main code:**********************/
		onInit: function() {
			if (window.File && window.FileList && window.FileReader) {
				fileDragUpload.fileDragInit();
			}
			else {
				alert("No File API");
			}
		},		

		fileDragInit: function() {
			// var fileselect = fileDragUpload.$id("fileselect");
			// fileselect.addEventListener("change", fileDragUpload.FileSelectHandler, false); // this makes an ajax call right after the file is selected, without pressing the button.
			// fileselect.style.display = "none";
			// var submitbutton = fileDragUpload.$id("submitbutton");
			// submitbutton.style.display = "none";
			
			 var filedrag = fileDragUpload.$id("filedrag");
			//var filedrag = fileDragUpload.$id("filelisttable");
			
						
			var xhr = new XMLHttpRequest();
			if (xhr.upload) { // only in XHR2, in Firefox, Chrome, it is available.
				// drop:
				filedrag.addEventListener("dragover", fileDragUpload.FileDragHover, false);
				filedrag.addEventListener("dragleave", fileDragUpload.FileDragHover, false);
				filedrag.addEventListener("drop", fileDragUpload.FileSelectHandler, false);
				filedrag.style.display = "block";	
			}
			else {
				alert("No XMLHttpRequest upload available.");
			}
		},
		
		FileDragHover: function(e) {
			// e.target is the filedrag element
			e.stopPropagation();
			e.preventDefault();
			e.target.className = (e.type == "dragover" ? "hover" : "");
		},

		FileSelectHandler: function(e) {
			// cancel event and hover styling
			fileDragUpload.FileDragHover(e);
			// fetch FileList object
			var files = e.target.files || e.dataTransfer.files;
			// process all File objects
			//fileDragUpload.upload2(files[0]); // OLD: only processes 1 file.
			var filesLength = files.length;
			for (var i= 0; i < filesLength; i++) {
				console.log(files[i]);
				fileDragUpload.upload2(files[i]);
			}
		},

		// http://blog.new-bamboo.co.uk/2012/01/10/ridiculously-simple-ajax-uploads-with-formdata
		upload2: function(file) {
			var parentPath = fileDragUpload.$id("parentpath").value;
			var data = new FormData();
			data.append("fileselect", file); // NOTE: we are sending one file at a time.
			data.append("parentpath", parentPath);
			var xhr = new XMLHttpRequest();
			if (xhr.upload) {
				xhr.onreadystatechange = function() {
					if (xhr.readyState == 4 && xhr.status == 200) {
						if (xhr.responseText) {
							//alert(xhr.responseText);
							var response = JSON.parse(xhr.responseText);
							if (response.success === false) {
								fileDragUpload.outputMessage("<p>Error uploading " + response.filename + ": " + response.errorMessage +"</p>");
							}	
							else {
								fileDragUpload.outputMessage("<p>File " + response.filename + " uploaded successfully.</p>");
								if (typeof fileDragUpload.callback !== "undefined") {
									fileDragUpload.callback(response.fileList); // updateFileList should be passed as callback.
								}
							}
						}
						else {
							fileDragUpload.outputMessage("No response from server.");
						}
					}
				};
				xhr.open("POST", fileDragUpload.$id("uploadForm").action, true);
				xhr.timeout = 20000;
				xhr.ontimeout = function() {
					fileDragUpload.outputMessage("<p>Sending file "+ file.name +" timed out.</p>"); 
				};
				xhr.send(data);
			}
		}

};