angular.module('chooseProviderApp', []);

function ChooseProviderCtrl($scope, $http, $window) {
	$scope.onSelectProvider = function(provider) {
		//alert("in choooseProvider: " + $window.location.href);
		var url = "/selectuser.html?provider=" + provider;
		$window.location.href = url;
	};
	
	/** Main function*/
	// nothing 
}