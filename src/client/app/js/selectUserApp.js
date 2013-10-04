angular.module('selectUserApp', ['selectUserFilters']);

function SelectUserCtrl($scope, $http, $location, $window, $routeParams) {	
	$scope.onGoBtnClick = function() {
		//$location.path("/muboxindex.html?uid=" + $scope.currentUid);
		//alert("in selectUserApp: " + $window.location.href);
		var url = "";
		if ($scope.provider) {
			url = "/muboxindex.html#/?provider=" + $scope.provider;
			if ($scope.currentUid) { 
				url += "&uid=" + $scope.currentUid;
			}
		}
		else {
			if ($scope.currentUid) {
				url = "/muboxindex.html#/?uid=" + $scope.currentUid;
			}
			else {
				url =  "/muboxindex.html";
			}			
		}
		$window.location.href  = url;
	};
		
	$scope.getParameter = function(queryString, paramName) {
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
	
	/** Main function */
	var queryString = $window.location.search.substring(1);
	$scope.provider = $scope.getParameter(queryString, "provider");
	if ($scope.provider === null) {
		$scope.provider = "dropbox"; // default
	}
	$http.get("../../../listusers?provider=" + $scope.provider).success(function(data) {	
		$scope.userList = data;
		// Can uncomment this to make sure we always select someone
		// $scope.currentUid = $scope.userList[0].uid;
	});
	
}


// SelectUserCtrl.$inject = ['$scope', '$http'];

angular.module('selectUserFilters', []).
filter('isCurrentSelected', function() {
	return function(uid) {
		if (uid) {
			return "other_option";
		}
		else {
			return "disabled";
		}
	}
});