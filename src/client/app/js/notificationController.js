function NotificationCtrl($scope, $timeout, $http, $rootScope) {
	// 1. sign up for updates;
	// 2. same --> when we change route (on destroy) --> unsubscribe from updates
	// 3. when updates arrive, push them into notifications. (TODO: check their votingID (or fileID???) first.
	
	$scope.closeAlert = function(index) {
		var notification = $scope.notifications[index];
		$scope.notifications.splice(index, 1);
		
		if (notification.votingID) {
			// This is a VotingClosedNotification. The user closes it, don't show it again.
			// The user must be the initiator (it is seen from the existing isAccepted property)
			if (typeof notification.isAccepted !== "undefined" && notification.isAccepted !== null) {
				$http.get('../../../removevoting?votingid=' + notification.votingID).success(function(data) {
					
				});
			}
			// else it is a VotingRequest. Do not remove it from DB.
		}
		else {
			// this is a "file change" notification that does not require voting.
			// Send it to server --> TODO: remove from DB?? WHICH table?? Where do we store it??
		}
	};
	
	$scope.removeFromNotificationList = function(voting) {
		var index = $scope.notifications.indexOf(voting);
		$scope.notifications.splice(index, 1);
	};
	
	$scope.vote = function(voting, accepted) {
		var requestData  = {};
		requestData.uid = $scope.useruid;
		requestData.accepted = accepted;
		requestData.votingID = voting.votingID;
		requestData.parentPath = $scope.parentPath;
		// original content-type: application/x-www-form-urlencoded
		$http.post('../../../vote', requestData, {headers:  {'Content-Type': 'application/x-www-form-urlencoded'}, timeout: 60000})  //application/x-www-form-urlencoded // multipart/form-data
		.success(function(responseData) {
			$scope.ref.fileList = responseData.fileList;	
			$scope.removeFromNotificationList(voting);
		})
		.error(function(responseData) {
			alert("error: " + responseData);
		});
	};
	
	$scope.showAcceptRejectButtons = function(notification) {
		return notification.votingID && notification.votingID !== -1 &&
		(typeof notification.isAccepted === "undefined" || notification.isAccepted === null);
	};
	
	$scope.accept = function(index) {
		// set voted = true in votingUsers, update votesFor++ in voting
		var notification = $scope.notifications[index];
		$scope.vote(notification, true);
	};
	
	$scope.reject = function(index) {
		// set voted = false in votingusers, update votesAgainst++ in voting
		var notification = $scope.notifications[index];
		$scope.vote(notification, false);
	};
	
	$scope.appendToNotificationList = function(list) {		
		var notifLen = $scope.notifications.length;
		var listLen = list.length;
		for (var i = 0; i < listLen; i++) {
			var contains = false;
			for (var j = 0; j < notifLen; j++) {
				if (list[i].votingID === $scope.notifications[j].votingID) {
					contains = true;
					break;
				}
			}
			if (!contains) {
				$scope.notifications.push(list[i]);
			}
		}
	};
	
	$scope.setUpNotifications = function() {
		var updating = {};
		var getNotifications = function() {
			if (!$scope.settings || !$scope.settings.disableVoting) {
				var requestData = {}
				requestData.parentPath = encodeURIComponent($scope.parentPath);
				$http.post('../../../notifications', requestData, {headers: {'Content-Type': 'application/x-www-form-urlencoded'}})
				.success(function(data) {
					var fileList = data.fileList;
					var newNotifications = data.notificationList;
					if (newNotifications.length !== 0) { // refresh the fileList if something was accepted (matters for suggestXxx) or rejected (matters, e.g., "rename shared")
						$scope.ref.fileList = fileList;
					}
					//alert("fileList length: " + fileList.length + ", newNotifications length: " + newNotifications.length);
					$scope.appendToNotificationList(newNotifications);
				})
				.error(function(data) {
					//alert("error getnotifications: " + data);
					console.log("error getnotifications: " + data);
				});
				updating = $timeout(getNotifications, 20000);
			}
			else {
				console.log("notifications disabled, will not repeat");
			}
		};
		updating = $timeout(getNotifications, 1000);
		$scope.$on('$destroy', function() {
			$timeout.cancel(updating);
		});
	};
	
	/****************** NotificationCtrl 'Main function' ***********************/
	if (typeof $scope.notifications === "undefined") {
		console.log("notifications undefined");
		$scope.notifications = [];
	}
	$scope.setUpNotifications();
	
} // end NotificationCtrl controller

// NOTE: may be needed if you want to compress your JS.
// NotificationCtrl.$inject = ['$scope', '$http', '$location', '$window', '$routeParams'];