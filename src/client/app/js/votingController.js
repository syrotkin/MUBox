function VotingCtrl($scope, $http, $routeParams) {
	
	$scope.mouseover = function(index) {
		$scope.closeButtonColors[index] = "#ff0000";
	};
	
	$scope.mouseout = function(index) {
		$scope.closeButtonColors[index] = "#000000";
	};
	
	// finds the vote by its votingID. This is needed because the votes may be sorted 
	// differently in the UI.
	$scope.findVoteByID = function(votingID) {
		var index = -1;
		var length = $scope.votingList.length;
		for (var i = 0; i < length; i++) {
			if ($scope.votingList[i].votingID === votingID) {
				index = i;
				break;
			}
		}
		return index;
	};
	
	$scope.closeAlert = function(votingID) {
		var index = $scope.findVoteByID(votingID);
		var openVote = {};
		if (index !== -1) {
			openVote = $scope.votingList[index];
			$scope.votingList.splice(index, 1);
		}
		if (typeof openVote.isAccepted !== "undefined" &&
				openVote.isAccepted !== null &&
				openVote.initiator === $scope.useruid) { // only the initiator may delete the "closed" notification.
			$http.get('../../../removevoting?votingid=' + votingID)
			.success(function(data) {
				
			});
		}
	};
	
	$scope.vote = function(openVote, accepted) {
		var requestData = {};
		requestData.uid = $scope.useruid;
		requestData.accepted = accepted;
		requestData.votingID = openVote.votingID;
		requestData.parentPath = "/"; // this is irrelevant here
		$http.post('../../../vote', requestData, {headers: {'Content-Type': 'application/x-www-form-urlencoded'}, timeout: 60000})
		.success(function(responseData) {
			var index = $scope.votingList.indexOf(openVote);
			$scope.votingList.splice(index, 1);
		})
		.error(function(responseData) {
			alert("error voting: " + responseData);
		});
	};
	
	$scope.accept = function(votingID) {
		var index = $scope.findVoteByID(votingID);
		if (index !== -1) {
			var openVote = $scope.votingList[index];
			$scope.vote(openVote, true);
		}
	};
	
	$scope.reject = function(votingID) {
		var index = $scope.findVoteByID(votingID);
		if (index !== -1) {
			var openVote = $scope.votingList[index];
			$scope.vote(openVote, false);
		}
	};
		
	/** Main function*/
	$scope.useruid = $routeParams.uid;
	$scope.predicate = "voted && isAccepted";
	$scope.titles = ['File Operation', 'Initiator', 'Old Path', 'New Path', 
	                 'Voted?', 'Votes In Favor', 'Votes Against',
	                 'Total User Count', 'Voting Scheme', 'Accepted?'];
	$http.get("../../..//openvotes/" + $scope.useruid)
	.success(function(data) {
		$scope.votingList = data;
		$scope.closeButtonColors = [];
		var length = $scope.votingList.length;
		for (var i = 0; i < length; i++) {
			$scope.closeButtonColors.push("#000000");
		}
	});
} // end VotingCtrl

//VotingCtrl.$inject = ['$scope', '$http', '$routeParams'];