
function FileCtrl($scope, $http, $location, $window, $routeParams, $timeout, $filter, $rootScope, $document) {
	
	/******************Listing methods******************/
	
	$scope.isSharedActionPending = function(file) {
		return file.creationAction === "renameshared" || file.creationAction == "moveshared" || file.deletionAction === "deleteshared";
	};
	
	$scope.isRestoreButtonVisible = function() {
		var result = $scope.currentFile && 
		($scope.currentFile.creationAction === "move" || 
		$scope.currentFile.creationAction === "rename" ||
		$scope.currentFile.deletionAction === "delete");
		if (result) {
			if ($scope.currentFile.creationAction === "move") {
				$scope.restoreButtonLabel = "Undo move";
			}
			else if ($scope.currentFile.creationAction === "rename") {
				$scope.restoreButtonLabel = "Undo rename";
			}
			else if ($scope.currentFile.deletionAction === "delete") {
				$scope.restoreButtonLabel = "Undo delete";
			}
			else {
				$scope.restoreButtonLabel = "Restore"; // should not matter
			}
		}
		else {
			$scope.restoreButtonLabel = "Restore"; // should not matter
		}
		return result;
	};
	
	$scope.isRenameButtonVisible = function() {
		return $scope.currentFile && 
		!$scope.currentFile.isDeleted && 
		(($scope.currentFile.isShared !== true &&
		!$scope.isSharedActionPending($scope.currentFile)) || $scope.settings.disableVoting === true);
	};
	
	$scope.isRenameSharedButtonVisible = function() {
		return $scope.currentFile && 
		$scope.currentFile.isDeleted === false && 
		$scope.currentFile.isShared === true &&
		$scope.currentFile.owner === $scope.useruid &&
		typeof $scope.currentFile.sharedUsers === "undefined" && // exclude the top-level shared folder
		!$scope.isSharedActionPending($scope.currentFile) && 
		$scope.settings.disableVoting === false;
	};
	
	$scope.isSuggestRenameButtonVisible = function() {
		return $scope.currentFile &&
		$scope.currentFile.isDeleted === false &&
		$scope.currentFile.isShared === true &&
		$scope.currentFile.owner !== $scope.useruid &&  // the current user is not the owner (original sharer)
		typeof $scope.currentFile.sharedUsers === "undefined" && // it is not the top-level shared folder
		!$scope.isSharedActionPending($scope.currentFile) && 
		$scope.settings.disableVoting === false;
	};
	
	$scope.isShareFolderButtonVisible = function() {
		return  $scope.currentFile && 
		!$scope.currentFile.isDeleted && 
		$scope.currentFile.isDir &&
		$scope.currentFile.isShared === false;
	};
	
	$scope.isManageSharedFolderButtonVisible = function() {
		return $scope.currentFile &&
		!$scope.currentFile.isDeleted &&
		$scope.currentFile.isDir &&
		$scope.currentFile.isShared === true &&
		$scope.useruid === $scope.currentFile.owner &&
		typeof $scope.currentFile.sharedUsers !== "undefined" && // only include top-level shared folders
		!$scope.isSharedActionPending($scope.currentFile);
	};
	
	$scope.isDownloadButtonVisible = function() {
		return $scope.currentFile && !$scope.currentFile.isDeleted && !$scope.currentFile.isDir &&
		!$scope.isSharedActionPending($scope.currentFile);
	};
	
	$scope.isShowVersionsButtonVisible = function() {
		return $scope.currentFile && !$scope.currentFile.isDeleted && !$scope.currentFile.isDir &&
		!$scope.isSharedActionPending($scope.currentFile);
	};
	
	$scope.isDeleteButtonVisible = function() {
		return $scope.isRenameButtonVisible();
	};

	$scope.isDeleteSharedButtonVisible = function() {
		return $scope.isRenameSharedButtonVisible();
	};

	$scope.isSuggestDeleteButtonVisible = function() {
		return $scope.isSuggestRenameButtonVisible();
	};
	
	$scope.isCutButtonVisible = function() {
		return $scope.currentFile && !$scope.currentFile.isDeleted &&
		// TODO: prevent cutting top-level shared folders
		!$scope.isSharedActionPending($scope.currentFile);
	};
	
	$scope.isCopyButtonVisible = function() {
		return $scope.currentFile && !$scope.currentFile.isDeleted &&
		// TODO: ?? Prevent copying top-level shared folders??
		!$scope.isSharedActionPending($scope.currentFile);
	};
	
	/**
	 * True if the currently highlighted file is a folder and is different from the file in the clipboard
	 */
	$scope.isValidCurrentFileForPasting = function() {
		return $scope.currentFile &&
		$scope.currentFile.isDir &&
		$scope.currentFile.path !== $scope.clipboardData.path;
	};
	
	/** Currently selected file 'Paste' button**/
	/**
	 * True if the currently highlighted (selected) folder is shared and suitable for pasting
	 */
	$scope.canPasteInCurrentSharedFolder = function() {
		return $scope.clipboardData &&
		$scope.clipboardData.isShared && $scope.clipboardData.op === 'cut' && $scope.settings.disableVoting === false &&		
		$scope.isValidCurrentFileForPasting();
	};
	$scope.isPasteButtonCurrentVisible = function() {
		return $scope.clipboardData &&
		(!$scope.clipboardData.isShared || $scope.clipboardData.op !== 'cut' || $scope.settings.disableVoting === true) &&
		$scope.isValidCurrentFileForPasting();
	};
	$scope.isPasteSharedButtonCurrentVisible = function() {
		return $scope.canPasteInCurrentSharedFolder() &&
		$scope.clipboardData.isOwner;
	};
	$scope.isSuggestPasteButtonCurrentVisible = function() {
		return $scope.canPasteInCurrentSharedFolder() &&
		!$scope.clipboardData.isOwner;
	};
	
	/** Top-level 'Paste' button **/
	/**
	 * True if the currently open (listed) folder is shared and suitable for pasting 
	 */
	$scope.canPasteInTopLevelSharedFolder = function() {
		return $scope.clipboardData && 
		$scope.clipboardData.isShared && $scope.clipboardData.op === 'cut' && $scope.settings.disableVoting === false &&
		$scope.parentPath !== $scope.getParentPath($scope.clipboardData.path) &&
		$scope.parentPath !== $scope.clipboardData.path;
	};
	
	$scope.isPasteButtonTopLevelVisible = function() {
		return $scope.clipboardData && 
		(!$scope.clipboardData.isShared || $scope.clipboardData.op !== 'cut' || $scope.settings.disableVoting === true) &&
		$scope.parentPath !== $scope.getParentPath($scope.clipboardData.path) &&
		$scope.parentPath !== $scope.clipboardData.path;
		//return false;
	};
	
	$scope.isPasteSharedButtonTopLevelVisible = function() {
		return $scope.canPasteInTopLevelSharedFolder() &&
		$scope.clipboardData.isOwner;
		//return false;
	};
	
	$scope.isSuggestPasteButtonTopLevelVisible = function() {
		return $scope.canPasteInTopLevelSharedFolder() &&
		!$scope.clipboardData.isOwner;
		// return false;
	};
	
	
	$scope.isCancelVoteButtonVisible = function() {
		return $scope.currentFile &&
		$scope.isSharedActionPending($scope.currentFile);
	};
	
	
	$scope.restoreFile = function() {
		$http.get('../../../restore' + $scope.currentFile.path).success(function(data) {
			if (data.error) {
				alert("ERROR restoring: " + data.error);
			}
			else {
				$scope.ref.fileList = data.fileList;
			}
		});
	};
	
	$scope.setCurrent= function(file) {
		$scope.currentFile = file;
	};
	
	$scope.showTooltip = function() {
	};
	
	$scope.onlyInactive = function(item) {
		return item.active === false; 
	};
	
	$scope.onlyActive = function(item) {
		return item.active === true;
	};
		
	$scope.initShareFolderModal = function() {
		$scope.votingSchemes = [{label: 'Majority', name:'majority'},
		                        {label:'Majority with time constraint', name:'majoritytimeconstraint'},
		                        {label:'Percentage', name:'percentage'},
		                        {label:'Percentage with time constraint', name:'percentagetimeconstraint'},
		                        {label:'Veto with time constraint', name:'vetotimeconstraint'}];
		$http.get("../../../listusers?provider=" + $scope.provider).success(function(data) {
			$scope.userList = data;
		});
	};
	
	$scope.showShareFolderModal = function() {
		$scope.initShareFolderModal();
		$scope.shareFolderModalTitle = "Share folder " + $scope.currentFile.filename  +":";
		$scope.userListTitle = "Select Users:";
		$scope.shareUpdateFolderLabel = "Share";
		$scope.selectedVotingScheme = "majority";
		$("#shareFolderModal").modal('show');
	};
	
	$scope.showManageSharedFolderModal = function() {
		$scope.initShareFolderModal();
		$scope.shareFolderModalTitle  = "Manage shared folder " + $scope.currentFile.filename  +":";
		$scope.userListTitle = "Selected Users:";
		$scope.selectedUserList = [];
		var length = $scope.currentFile.sharedUsers.length;
		for (var i = 0; i < length; i++) {
			$scope.selectedUserList.push($scope.currentFile.sharedUsers[i].uid);
		}
		$http.get('../../../getsharedfolderinfo/' + $scope.currentFile.sharedFolderID)
		.success(function(data) {
			$scope.selectedVotingScheme = data.votingScheme;
			$scope.percentUsers = data.percentage;
			$scope.timeUnit = data.timeUnit;
			$scope.numTimeUnits = data.numTimeUnits;
		})
		.error(function(data) {
			alert("error getting sharedfolderinfo: " + data)
		});
		$scope.shareUpdateFolderLabel = "Update Folder";
		$("#shareFolderModal").modal('show');
	};
	
	$scope.sendVote = function(requestData) {
		$http.post('../../../vote', requestData, {headers:  {'Content-Type': 'application/x-www-form-urlencoded'}})  //application/x-www-form-urlencoded // multipart/form-data
		.success(function(responseData) {
			$scope.ref.fileList = responseData.fileList;
			$scope.currentFile = null;
		})
		.error(function(responseData) {
			alert("error: " + responseData);
		});
	};
	
	$scope.cancelVote = function() {
		var votingInput = {};
		votingInput.votingID = $scope.currentFile.votingID;
		votingInput.uid = $scope.useruid;
		votingInput.accepted = false;
		votingInput.cancel = true;
		votingInput.parentPath = $scope.parentPath;
		$scope.sendVote(votingInput);
	};
	
	$scope.isTimePeriodVisible = function() {
		return $scope.selectedVotingScheme === 'majoritytimeconstraint' || 
		$scope.selectedVotingScheme === 'percentagetimeconstraint' || 
		$scope.selectedVotingScheme === 'vetotimeconstraint';
	};
	
	$scope.isPercentUsersVisible = function() {
		return $scope.selectedVotingScheme === 'percentage' || 
		$scope.selectedVotingScheme === 'percentagetimeconstraint';
	};
	
	$scope.disableShareUpdateFolderButton  = function() {
		return !$scope.selectedVotingScheme || 
		($scope.isPercentUsersVisible() && !$scope.percentUsers) ||
		($scope.isTimePeriodVisible() && (!$scope.timeUnit || !$scope.numTimeUnits));
	};
	
	$scope.shareFolder = function() {
		var userUids = $scope.selectedUserList;
		var path = $scope.currentFile.path;
		var data = {};
		var users = [];
		var userUidsLength = userUids.length;
		for (var i = 0; i < userUidsLength; i++) {
			users.push({uid: userUids[i], path: '/' + $scope.currentFile.filename});
		}
		users.push({uid: $scope.useruid, path: path})
		data = {users: users, 
			scheme: $scope.selectedVotingScheme,
		};
		if (!$scope.isTimePeriodVisible()) {
			$scope.numTimeUnits = undefined;
			$scope.timeUnit = undefined;
		}
		if (!$scope.isPercentUsersVisible()) {
			$scope.percentUsers = undefined;
		}
		
		if ($scope.numTimeUnits && $scope.timeUnit) {
			data.periodInMinutes = $scope.numTimeUnits * $scope.timeUnit;
		}
		else {
			data.periodInMinutes = -1;
		}
		if ($scope.percentUsers) {
			data.percentage = $scope.percentUsers;
		}
		else {
			data.percentage = -1;
		}
		
		//var dataAsString = JSON.stringify(data);
		$http.post('../../../sharefolder', data, {headers:  {'Content-Type': 'application/x-www-form-urlencoded'}})  //application/x-www-form-urlencoded // multipart/form-data
		.success(function(data) {
			$scope.ref.fileList = data.fileList;
			$("#shareFolderModal").modal('hide');
		});
	};
	
	// TODO: handle folders -- zip and download. Otherwise, delete this function
	$scope.downloadFile = function() {
		$window.open('../../../download' + $scope.currentFile.path);
	};
	
	$scope.directDownloadFile = function() {
		var file = $scope.currentFile;
		// We assume that on folders this option will not be visible
		var link = "";
		if (typeof file === "undefined") {
			link = "#";
		}
		else {
			var protocol = $window.location.protocol;
			var host = $window.location.host
			if (file.isDeleted) {
				var revisionsURL = "#/revisions" + file.path;
				link = revisionsURL;
			}
			else {
				var downloadURL = protocol + "//" + host + "/directdownload" + file.path;
				link = downloadURL;  // "#" + file.path + '?time=' + currMinutes + '_' + currSeconds + '_' + currMilliseconds;
			}
		}
		$window.open(link);
		$window.focus();
		return false;
	};
	
	$scope.doDeletion = function(action) {
		var requestData = {
					action: action, 
					path: encodeURIComponent($scope.currentFile.path) };
		$http.post('../../../delete', requestData, {headers: {'Content-Type': 'application/x-www-form-urlencoded'}, timeout: 20000})
		.success(function(responseData) {
			$scope.ref.fileList = responseData.fileList;
		})
		.error(function(responseData) {
			alert("delete error: " + responseData);
		});
	};
	/*
	$scope.deleteFile = function() {
		// TODO: Are you sure dialog
		$scope.doDeletion("delete");
	};
	
	$scope.deleteSharedFile = function() {
		$scope.doDeletion("deleteshared");
	};
	
	$scope.suggestDeleteFile = function() {
		$scope.doDeletion("suggestdelete");
	};
	*/
	
	$scope.showRenameDialog = function() {
		$scope.newFileName = "";
		$('#renameModal').on('shown', function () {
		    $('#newname').focus();
		})
		$("#renameModal").modal('show');
	};
	
	$scope.renameFile = function() {
		// TODO: disable renaming if illegal characters entered
		$scope.renameDialogHeader = "New name for " + $scope.currentFile.filename +":";
		$scope.showRenameDialog();
	};
	
	$scope.renameSharedFile = function() {
		$scope.renameDialogHeader = "New name for the shared file " + $scope.currentFile.filename +":";
		$scope.isRenamingShared = true;
		$scope.showRenameDialog();
	};
	
	$scope.suggestRenameFile = function() {
		$scope.renameDialogHeader = "Suggest a new name for " + $scope.currentFile.filename + ":";
		$scope.isSuggestRename = true;
		$scope.showRenameDialog();
	};
	
	/** Finds the index of the file with the given name in $scope.ref.fileList */
	$scope.findName = function(name) {
		var length = $scope.ref.fileList.length;
		for (var i = 0; i< length; i++) {
			if ($scope.ref.fileList[i].filename === name) {
				return i;
			}
		}
		return -1;
	};
	
	$scope.renameEnterPress = function($event) {
		$scope.saveNewName();
	};
	
	$scope.saveNewName = function() {
		$("#renameModal").modal('hide');
		var isRenamingShared = $scope.isRenamingShared;
		$scope.isRenamingShared = false;
		var isSuggestRename = $scope.isSuggestRename;
		$scope.isSuggestRename = false;
		var name = $scope.newFileName; // using ng-model
		// TODO: enable renaming if only the case differs??
		if (name) { // TODO: if name is different from the previous name!!!
			var url = "";
			if (isRenamingShared) {
				url = '../../../renameshared';
			}
			else if (isSuggestRename) {
				url = '../../../suggestrename';
			}
			else {
				url = '../../../rename';
			}
			$http.get(url + $scope.currentFile.path + "?newname=" + name)
			.success(function(data) {
				$scope.ref.fileList = data.fileList;
				var index = $scope.findName(name);
				if (index !== -1) {
					$scope.currentFile = $scope.ref.fileList[index];
				}
			});
		}
	};
	
	$scope.putCurrentFileIntoClipboard = function() {
		$scope.clipboardData = { 
				path: $scope.currentFile.path, 
				filename: $scope.currentFile.filename,  
				isShared: $scope.currentFile.isShared,
				isOwner: $scope.currentFile.owner === $scope.useruid };
	};
	
	$scope.copyFile = function() {
		$scope.putCurrentFileIntoClipboard();
		$scope.clipboardData.op = 'copy';
		if (Storage) {
			sessionStorage.clipboardData = JSON.stringify($scope.clipboardData);
		}
	};

	$scope.cutFile = function() {
		$scope.putCurrentFileIntoClipboard();
		$scope.clipboardData.op = 'cut';
		if (Storage) {
			sessionStorage.clipboardData = JSON.stringify($scope.clipboardData);
		}
	};
	
	$scope.toggleDeleted = function() {
		$scope.deletedVisible = !$scope.deletedVisible;
		if (Storage) {
			sessionStorage.deletedVisible = $scope.deletedVisible.toString();
		}
	};
	
		
	// Syncs with Dropbox
	$scope.syncCloudStorage = function() {
		$scope.syncing = true;
		$http.get('../../../sync' + $scope.parentPath).success(function(data) {
			$scope.ref.fileList = data.fileList;
			$scope.syncing = false;
		});
	};

	$scope.paste = function(fromPath, toPath, action) {
		var data = {from: encodeURIComponent(fromPath), to: encodeURIComponent(toPath), parentPath: encodeURIComponent($scope.parentPath), action: action};
		console.log("from: " + fromPath + "; to: " + toPath + "; encoded: " + encodeURIComponent(fromPath) + ", encoded: " + encodeURIComponent(toPath));
		var url = '';
		if ($scope.clipboardData.op === 'copy') {	
			url = '../../../copyfile';
		}
		else { // 'cut';
			url = '../../../movefile';
		}
		$http.post(url, data, {headers:  {'Content-Type': 'application/x-www-form-urlencoded'}, timeout: 10000})
		.success(function(responseData) {
			if (responseData.fileList) {
				$scope.ref.fileList = responseData.fileList;
			}
			else if (responseData.error) {
				alert("An error occurred: " + responseData.error);
			}
			else {
				alert("Could not copy/move the file.");
			}
		})
		.error(function(responseData) {
			alert("error: " + responseData);
		});

		$scope.clipboardData= null;
		sessionStorage.clipboardData = null;
	};
		
	/**
	 * Paste copied/cut file into the currently listed directory
	 */
	$scope.onPasteTopLevel = function() { 
		var fromPath = $scope.clipboardData.path;
		var toPath = $scope.parentPath + '/' + $scope.clipboardData.filename;
		$scope.paste(fromPath, toPath, "paste");
	};
	$scope.onPasteSharedTopLevel = function() {
		var fromPath = $scope.clipboardData.path;
		var toPath = $scope.parentPath + '/' + $scope.clipboardData.filename;
		$scope.isPasteShared = true;
		$scope.paste(fromPath, toPath, "pasteshared");
	};
	
	$scope.onSuggestPasteTopLevel = function() {
		var fromPath = $scope.clipboardData.path;
		var toPath = $scope.parentPath + '/' + $scope.clipboardData.filename;
		$scope.isSuggestPaste = true;
		$scope.paste(fromPath, toPath, "suggestpaste");
	};
	
	/**
	 * Paste copied/cut file into the currently selected (highlighted in the table) directory
	 */
	$scope.onPasteCurrent = function() {
		var fromPath = $scope.clipboardData.path;
		var toPath = $scope.currentFile.path + '/' + $scope.clipboardData.filename;
		$scope.paste(fromPath, toPath, "paste");
	};
	
	$scope.onPasteSharedCurrent = function() {
		var fromPath = $scope.clipboardData.path;
		var toPath = $scope.currentFile.path + '/' + $scope.clipboardData.filename;
		$scope.isPasteShared = true;
		$scope.paste(fromPath, toPath, "pasteshared");
	};
	
	$scope.onSuggestPasteCurrent = function() {
		var fromPath = $scope.clipboardData.path;
		var toPath = $scope.currentFile.path + '/' + $scope.clipboardData.filename;
		$scope.isSuggestPaste = true;
		$scope.paste(fromPath, toPath, "suggestpaste");
	};
		
	$scope.getParentPath = function (path) {
		var parentPath = null;
		if (path.indexOf('/') === path.lastIndexOf('/')) {
			parentPath = "/";
		} else {
			var lastIndex = path.lastIndexOf('/');
			parentPath = path.substring(0, lastIndex);
		}
		return parentPath;
	};
	
	$scope.onLogOut = function() {
		$scope.clipboardData = null;
		$scope.settings = null;
		sessionStorage.clear();
		/*
		sessionStorage.clipboardData = null;
		sessionStorage.settings = null;
		*/
		$window.location.href = "../../../logout";
	};
	
	$scope.onShowVersions = function() {
		var file  = $scope.currentFile;
		var link ="/revisions" + file.path;
		$location.path(link); // TODO: this triggers the controller. Can we trigger it in a different way? Goal: open revision page in a different tab
	};
	
	$scope.onShowUploadModal = function() {
		$("#uploadModal").modal('show');
		fileDragUpload.callback = $scope.updateFileList;
		fileDragUpload.onInit();
		$("#parentpath").val($scope.parentPath);
	};
	
	$scope.onShowNewFolderModal = function() {
		$scope.newFolderName = "";
		// Credit for setting focus: http://stackoverflow.com/questions/11634809/twitter-bootstrap-focus-on-textarea-inside-a-modal-on-click
		$('#newFolderModal').on('shown', function () {
		    $('#newFolderInput').focus();
		});
		$("#newFolderModal").modal('show');
	};
	
	$scope.newFolderEnterPress = function($event) {
		$scope.onNewFolder();
	};
	
	$scope.onNewFolder = function() {
		$("#newFolderModal").modal('hide');
		var name = $scope.newFolderName; //ng-model
		if (name) {
			var newFolderPath = $routeParams.filePath ?  '/' + $routeParams.filePath + '/' : '/';
			$http.get('../../../newfolder' + newFolderPath  + name)
			.success(function (data) {
				//$scope.ref.fileList.splice(0, 0, newFolder); // This used to return just the new folder
				$scope.ref.fileList = data.fileList;
			});
		}
	};
	
	$scope.getParentPathFromHash = function(hash) {
		if (hash.indexOf('/') === -1) {
			return "/";
		}
		else {
			var indexQueryString = hash.indexOf("?");
			if (indexQueryString === -1) {
				return hash.substring(1);
			}
			else {
				var result = hash.substring(1, indexQueryString); 
				return result;
			}
		}
	};
	
	$scope.updateFileList = function(fileList) {
		if (typeof fileList !== "undefined") {
			$scope.$apply(function () {	// because it is modified outside of angular (by drag)
				$scope.ref.fileList = fileList;
			});			
		}
	};
	
	$scope.setWindowTitle = function() {
		var title = "";
		var length = $scope.breadcrumbs.length;
		for (var i = 0; i < length; i++) {
			if ($scope.breadcrumbs[i].active === true) {
				title = $scope.breadcrumbs[i].name;
				break;
			}
		}
		if (title !== "") {
			$window.document.title = title + " - MUBox";
		}
		else {
			$window.document.title = "MUBox";
		}
	};
	
	$scope.listFiles = function() {
		var path = "/";
		if ($routeParams.filePath) {
			path = '/' + $routeParams.filePath;
		}
		var homePath = '../../../home2' + path; 		// so that we go to C:\dev\test\SparkTest because that is the starting directory
		var currentUserUid =  $scope.useruid || $routeParams.uid;
		var currentProvider = $routeParams.provider;
		if (currentUserUid) {
			homePath += "?uid=" + currentUserUid;
			if (currentProvider) {
				homePath += "&provider=" + currentProvider;
			}
		}
		else {
			if (currentProvider) {
				homePath += "?provider=" + currentProvider;
			}
		}
		$http.get(homePath).success(function(data) {
			if (data.url) { // authentication
				$window.location.href = data.url;
			}
			else {
				$scope.breadcrumbs = data.breadcrumbs;
				$scope.ref.fileList = data.fileList;
				
				$scope.setWindowTitle();
				
				// sets up gray and green background for deleted/shadow files and new files respectively.
				$scope.setUpShadowFiles();
				$scope.setUpNewFiles();
				
				$scope.useruid = data.user.uid;
				$scope.userName = data.user.display_name;
				$scope.provider = data.provider;
			}
		})
		.error(function(data) {
			alert("error getting /home2 data: " + data);
		});
	};
		
	$scope.setUpDeletedVisible = function() {
		if (Storage && sessionStorage.deletedVisible) {
			$scope.deletedVisible = sessionStorage.deletedVisible === 'true' ? true : false;
		}
		else {
			$scope.deletedVisible = false;
			if (Storage) {
				sessionStorage.deletedVisible = $scope.deletedVisible.toString();
			}
		}
	};
	
	// Re-render "modified date" every 60s.
	$scope.setUpDateRendering = function() {
		var updating = {};
		var renderDates = function() {
			var formatFilter = $filter('formatDateAsAgo');
			var fileListLength = $scope.ref.fileList.length;
			for (var i = 0; i < fileListLength; i++) {
				formatFilter($scope.ref.fileList[i].date);
			}
			// variable updating will exist at this time:
			updating = $timeout(renderDates, 60000);
		};
		updating = $timeout(renderDates, 60000);
		// Canceling when navigating away
		$scope.$on("$destroy", function() {
			$timeout.cancel(updating);
		});
	};
	
	$scope.offGrayBackground = function() {
		var length = $scope.ref.fileList.length;
		for (var i = 0; i < length; i++) {
			var file = $scope.ref.fileList[i];
			if (file.isGrayBackground) {
				file.isGrayBackground = false;
			}
		}
	};
	$scope.onGrayBackground = function() {
		var now = new Date();
		var length = $scope.ref.fileList.length;
		for (var i = 0; i < length; i++) {
			var file = $scope.ref.fileList[i];
			if (file.isDeleted === true) {
				if (file.deletionAction === "move" || file.deletionAction === "rename" || 
						file.deletionAction === "moveshared" || file.deletionAction === "renameshared") {
					if (typeof file.lastSeen === "undefined" || file.lastSeen - file.deletionDate < 0) {
						file.isGrayBackground = true;
					}
				}
			}
		}
		$timeout($scope.offGrayBackground, 1000);
	};
	$scope.setUpShadowFiles = function() {
		$timeout($scope.onGrayBackground, 500);
	};
	
	$scope.offGreenBackground = function() {
		var length = $scope.ref.fileList.length;
		for (var i = 0; i < length; i++) {
			var file = $scope.ref.fileList[i];
			if (file.isGreenBackground) {
				file.isGreenBackground = false;
			}
		}
	};
	
	$scope.onGreenBackground = function() {
		var now = new Date();
		var length = $scope.ref.fileList.length;
		for (var i = 0; i < length; i++) {
			var file = $scope.ref.fileList[i];
			if (!file.isDeleted) {
				if (file.creationAction === "newfolder" || file.creationAction === "upload" || 
						file.creationAction === "copy" || file.creationAction === "move") {
					if (typeof file.lastSeen === "undefined" || file.lastSeen - file.creationDate < 0) {
						file.isGreenBackground = true;
					}
				}
			}
		}
		$timeout($scope.offGreenBackground, 1000);
	};
	
	$scope.setUpNewFiles = function() {
		$timeout($scope.onGreenBackground, 500);
	};
	
	$scope.getParameter = function(queryString, paramName) {
		if (!queryString) {
			return null;
		}
		var params = queryString.split('&');
		for (var i = 0; i < params.length; i++) {
			var position = params[i].indexOf('=');
			if (position > 0) {
				var property = params[i].substring(0, position);
				var value = params[i].substring(position + 1);
				if (property === paramName) {
					return value;
				}
			}
		}
		return null;
	};
	
	$scope.showMenu = function($event, file) {
		//alert(file.path);
		$scope.setCurrent(file);
		$scope.contextMenuDisplay = "block";
		$scope.contextMenuLeft = $event.clientX + "px";
		$scope.contextMenuTop = $event.clientY + "px";
	};
	
	$scope.fileDragHandler = function(e) {
		console.log("inside $scope.fileDragHandler");
		e.stopPropagation();
		e.preventDefault();
		var dragDiv = document.getElementById("dragDiv");
		if (e.type == "dragover") {
			dragDiv.style.borderColor = "green";
			dragDiv.style.borderWidth = "0.3em";
			dragDiv.style.borderStyle = "solid";
		}
		else {
			dragDiv.style.borderColor = "white";
			dragDiv.style.borderWidth = "0.3em";
			dragDiv.style.borderStyle = "solid";
		}
	};
	
	$scope.fileSelectHandler = function(e) {
		$scope.fileDragHandler(e);
		var files = e.target.files || e.dataTransfer.files;
		var filesLength = files.length;
		for (var i = 0; i < filesLength; i++) {
			$scope.doUpload(files[i]);
		}
	};
	
	$scope.doUpload = function(file) {
		var parentPath = document.getElementById("parentpath").value;
		var data = new FormData();
		data.append("fileselect", file);
		data.append("parentpath", parentPath);
		var xhr = new XMLHttpRequest();
		if (xhr.upload) {
			xhr.onreadystatechange = function() {
				if (xhr.readyState == 4 && xhr.status == 200) {
					if (xhr.responseText) {
						var response = JSON.parse(xhr.responseText);
						if (response.success === false) {
							alert("Error uploading " + response.filename + ": " + response.errorMessage);
						}
						else {
							// NOTE: could also display message: "<response.filename> uploaded successfully."
							$scope.updateFileList(response.fileList);
						}
					}
					else {
						alert("No response from server.");
					}
					$("#spinnerModal").modal('hide');
				}
			};
			xhr.open("POST", document.getElementById("uploadForm").action, true);
			xhr.timeout = 20000;
			xhr.ontimeout = function() {
				alert("Sending file " + file.name + " timed out.");
			};
			xhr.send(data);
			$("#spinnerModal").modal('show');
		}
	};
	
	$scope.dragDropInit = function() {
		if (!window.File || !window.FileList || !window.FileReader) {
			alert("No File API in this browser. Uploading not available.");
			return;
		}
		var dragDiv = document.getElementById("dragDiv");
		var xhr = new XMLHttpRequest();
		if (xhr.upload) {
			dragDiv.addEventListener("dragover", $scope.fileDragHandler, false);
			dragDiv.addEventListener("dragleave", $scope.fileDragHandler, false);
			dragDiv.addEventListener("drop", $scope.fileSelectHandler, false);
			// TODO: show the extra div, where you give feedback to user
		}
		else {
			alert("No XMLHttpRequest upload available!");
			return;
		}		
	};
	
	/************  FileCtrl Main function   ****************************/
	
	$scope.ref = {fileList: []};
	$scope.currentRouteParams = $routeParams;
	$scope.breadcrumbs = [{href:"/", name:"HOME", active:true}]; // initialization
	
	$scope.listFiles();
	
	$scope.setUpDateRendering();
	$scope.setUpDeletedVisible();

	if (Storage && sessionStorage.clipboardData) {
		$scope.clipboardData = JSON.parse(sessionStorage.clipboardData);
	}
	
	if (Storage && sessionStorage.settings) {
		$scope.settings = JSON.parse(sessionStorage.settings);
	}
	else {
		$http.get('../../../settings')
		.success(function(data) {
			$scope.settings = data;
			console.log($scope.settings);
			if (Storage) {
				//sessionStorage.clipboardData = JSON.stringify($scope.clipboardData);
				sessionStorage.settings = JSON.stringify($scope.settings);
			}
		})
		.error(function(data) {
			alert("Error receiving settings: " + data);
			$scope.settings = {disableShadow: false, disableActivityView: false, disableVoting: false};
		});
	}
	
	
	$scope.parentPath = $scope.getParentPathFromHash($window.location.hash);
	
	$scope.directDownloadURL = "#";
	if (typeof $scope.useruid === "undefined") {
		$scope.useruid = "";
	}

	$scope.contextMenuDisplay = "none";
	// Hacky way to attach a click to document:
	// We are using the $document service, which is good, but we are using a jQuery(?), basically non-Angular function .click()
	// That's why we have to call $scope.$apply() to tell Angular that a change took place
	$document.click(function() {
		$scope.$apply(function() {
			$scope.contextMenuDisplay = "none";
		});		
	});
	
	// Set up drag and drop
	$("#parentpath").val($scope.parentPath);
	$scope.dragDropInit();
	
	/*
	 	// Debugging:
		$scope.hostname = $window.location.hostname;
		$scope.host = $window.location.host;
		$scope.port = $window.location.port;
		$scope.windowLocationHref = $window.location.href;
		$scope.pathname = $window.location.pathname;
		$scope.protocol = $window.location.protocol;
		$scope.windowLocationHash = $window.location.hash;
	*/
	
} // end FileCtrl controller

//FileCtrl.$inject = ['$scope', '$http', '$location', '$window', '$routeParams'];
