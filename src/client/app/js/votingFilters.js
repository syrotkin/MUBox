angular.module('votingFilters', [])
.filter('getVotedMessage', function () {
	return function(voted) {
		if (voted === true) {
			return "You have voted";
		}
		else if (voted === false) {
			return "You need to vote";
		}
		else { // could be undefined
			return "";
		}
	};
})
.filter('setOpenVoteStyle', function () {
	return function(vote) {
		if (vote.voted === false || 
				(typeof vote.isAccepted !== "undefined" &&
						vote.isAccepted !== null)) {
			return "background-color:#CAE1FF;";
		}
		return "";
	};
})
.filter('getAcceptedMessage', function() {
	return function(isAccepted) {
		if (typeof isAccepted === "undefined" || isAccepted === null) {
			return "";
		}
		else {
			return isAccepted ? "Yes" : "No";
		}
	};
})
;