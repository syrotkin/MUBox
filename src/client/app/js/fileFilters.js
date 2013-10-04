angular.module('fileFilters', []).
filter('fileFolderIconSrc', function() {
	return function(file) {
		if (file && file.isDir) {
			if (file.isDeleted) {
				return "../img/files/folder_gray.gif"; 
			}
			else {
				if (file.sharedUsers) {
					return "../img/files/folder_user.gif";
				}
				else {
					return "../img/files/folder.gif";
				}
			}
		}
		else {
			if (file.isDeleted) {
				 return "../img/files/deleted_file.gif";
			}
			else {
				return "../img/files/page_white_text.gif";
			}
		}
	}
}).

filter('getFileKind', function() {
	return function(file) {
		var result = "";
		if (file.isDeleted) {
			if (file.deletionAction === "delete") {
				result += "deleted ";
			}
			else if (file.deletionAction === "move" || file.deletionAction === "rename" || 
					file.deletionAction === "moveshared" || file.deletionAction === "renameshared") {
				result += "shadow ";
			}
		}
		if (file.isDir) {
			if (file.sharedUsers) {
				result += "shared folder";
			}
			else {
				result += "folder";
			}
		}
		else {
			result += "file";
		}
		return result;
	};
}).

filter('getSharedUsersText', function() {
	return function(file) {
		if (file.sharedUsers) {
			var length = file.sharedUsers.length;
			if (length == 0) {
				return "No sharing information found";
			}
			else {
				return "Shared with ";
			}
		}
		else {
			return ""; // don't display anything 
		}
	};
}).
filter('getSharedUsersHtml', function() {
	return function(file, path) {
		if (file.sharedUsers) {
			var div = "<div>";
			var list = file.sharedUsers;
			var listLength = list.length;
			for (var i = 0; i < listLength; i++) {
				var currentUser = list[i];
				var link = "#/activity" + path + "?username=" + currentUser.display_name;
				var src = currentUser.img;
				var img = "";
				if (typeof src !== "undefined") {
					img = "<img src='" + src + "'></img>";
				}
				div+= "<p>" + img + " <a href=\"" + link +"\">" + currentUser.display_name + "</a></p>";
			}
			div+="</div>";
			return div;
		}
		else {
			return "";
		}
	};
}).

filter('isDeletedStyle', function() {
	return function(isDeleted) {
		return isDeleted ? "color:#A0A0A0;" : "";
	};
}).

filter('isDeletedAndCurrentStyle', function() {
	return function(file, currentFile) {
		var style = "";
		if (currentFile && file.path === currentFile.path) {
			style += "background-color:#CAE1FF;";
		}
		if (file.isGreenBackground) { // assume that these are non-deleted
			style += "background-color:#00FF00";
		}
		else { // assume that these are deleted
			if (file.isDeleted) {
				style += "color:#A0A0A0;";
			}
			if (file.isGrayBackground) {
				style += "background-color:#c7c7c7";
			}
		}
		return style;
	};
}).

filter('fileFolderLink', function($window) {
	return function(file) {
		if (typeof file === "undefined") {
			return "#";
		}
		if (file.isDir) {
			if (file.isDeleted) {
				if (file.deletionAction === "rename" || file.deletionAction === "move" || 
						file.deletionAction === "renameshared" || file.deletionAction === "moveshared") {
					return "#" + file.path; // can access moved/renamed directories.
				}
				else {
					return "#" + file.path; // can access deleted directories, too.
				}
			}
			else {
				return  "#" + file.path;
			}
		}
		else {
			var protocol = $window.location.protocol;
			var host = $window.location.host
			if (file.isDeleted) {
				var revisionsURL = "#/revisions" + file.path;
				return revisionsURL;
			}
			else {
				if (file.creationAction === "renameshared" || file.creationAction == "moveshared" || file.deletionAction === "deleteshared") {
					return "";
				}
				else {
					var downloadURL = protocol + "//" + host + "/directdownload" + file.path;
					return downloadURL;  // "#" + file.path + '?time=' + currMinutes + '_' + currSeconds + '_' + currMilliseconds;
				}
			}
		}
	};
}).

filter('locationLabel', function() {
	return function(file) {
		if (file.isDeleted) {
			if ((file.deletionAction === "rename" || file.deletionAction === "renameshared") && 
					file.newLink) {
				return "New name: ";
			}
			else if ((file.deletionAction === "move" || file.deletionAction === "moveshared" ) && file.newLink) {
				return "New location: ";
			}
			else {
				return "";
			}
		}
		else  {
			if ((file.creationAction === "move" || file.creationAction === "moveshared") &&
					file.newLink) {
				return "Old location: ";
			}
			else if (file.creationAction === "copy" && file.newLink) {
				return "Old location: ";
			}
			else if ((file.creationAction === "rename" || file.creationAction === "renameshared") &&
					file.newLink) {
				return "Old name: ";
			}
			return "";
		}
	};
}).

filter('directFileFolderLink', function($window) {
	return function(file) {
		if (file.isDir) {
			/*return  "#/files" + file.path; // "#" + file.path;*/
			return  "#" + file.path; // "#" + file.path;
		}
		else {
			var date = new Date();
			var protocol = $window.location.protocol;
			var host = $window.location.host
			var downloadURL = protocol + "//" + host + "/directdownload" + file.path;
			return downloadURL; //"#/../../../download" + file.path;  // "#" + file.path + '?time=' + currMinutes + '_' + currSeconds + '_' + currMilliseconds;
		}
		// TODO: HACK!! // http://stackoverflow.com/questions/1273554/html-link-that-forces-refresh
	}
}).
filter('fileFolderDirectLinkText', function() {
	return function(file) {
		if (file.isDir) {
			return "";
		}
		else {
			return file.filename;
		}
	}
}).
filter('fileFolderTarget', function() {
	return function(isDir) {
		return isDir ? "" : "_blank";
	}
}).
filter('isCurrent', function() {
	return function(file, currentFile) {
		if (file.isDir) {
			return "";
		}
		else {
			if (currentFile && file.path === currentFile.path) {
				return "Download";
			}
			else {
				return "";
			}
		}
	};
}).

filter('isRevisionDeleted', function() {
	return function(isDeleted) {
		return isDeleted ? "Deleted" : "Modified";
	};
}).

filter('isEmpty', function() {
	return function(list) {
		if (!list) {
			return "display:none";
		}
		else if (list.length === 0) {
			return "";
		}
		else {
			return "display:none";
		}
	};
}).
filter('getToggleDeletedLabel', function() {
	return function(deletedVisible) {
		return deletedVisible ? "Hide Deleted/Shadow" : "Show Deleted/Shadow";
	};
}).
filter('formatDateAsAgo', function() {
	return function(msecs) {
		if (typeof msecs !== "number") {
			return '';
		}
		var date = new Date(msecs);
		var now = new Date();
		var minuteInMsec = 1000 * 60;
		var hourInMsec = minuteInMsec * 60;
		var dayInMsec = hourInMsec * 24;
		var diffInMsec = now - date;
		var diffInMinutes = Math.round(diffInMsec / minuteInMsec);
		if (diffInMinutes < 60) {
			var text = "";
			if (diffInMinutes === 0) {
				text = "< 1 minute ago."
			}
			else if (diffInMinutes === 1)  {
				text = "1 minute ago.";
			}
			else {
				text = diffInMinutes + " minutes ago.";
			}
			return text;
		}
		var diffInHours = Math.round(diffInMsec / hourInMsec);
		if (diffInHours < 24) {
			return diffInHours + (diffInHours == 1 ? " hour" : " hours") + " ago.";
		}
		var diffInDays = Math.round(diffInMsec / dayInMsec);
		if (diffInDays < 7) {
			return diffInDays + (diffInDays == 1 ? " day" : " days") + " ago.";
		}
		var month = date.getMonth();
		month = month + 1; // because getMonth() is 0-11.
		if (month < 10) {
			month = "0" + month;
		}
		var hours = date.getHours();
		var amPm = "";
		if (hours == 0) {
			hours = 12;
			amPm = "AM";
		}
		else if (hours > 0 && hours < 12) {
			amPm = "AM";
		}
		else if (hours == 12) {
			amPm = "PM";
		}
		else {
			hours = hours - 12;
			amPm = "PM";
		}
		var minutes = date.getMinutes();
		if (minutes < 10) {
			minutes = "0" + minutes;
		}
		var result = date.getDate() + "." + month +"."+ date.getFullYear() + " " + hours + ":" + minutes + " " + amPm;
		return result;
		//return date.toLocaleString();
	};
});
/*
filter('getVotingAction', function() {
	return function(action) {
		if (action === "renameshared") {
			return "Renamed";
		}
		else {
			return action;
		}
	}; 
});
*/