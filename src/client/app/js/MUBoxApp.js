'use strict';

/* App Module */

angular.module('MUBox', ['fileFilters', 'activityFilters', 'votingFilters', 'ui.bootstrap', 'ui.directives']).config(['$routeProvider', function($routeProvider, $locationProvider) {
	//$locationProvider.html5Mode(true);
	$routeProvider.
	when('/voting/:uid', {templateUrl: 'voting.html', controller: VotingCtrl}).
	when('/revisions/*filePath', {templateUrl: 'revisions.html', controller: RevisionCtrl}).
	when('/activity/*filePath', {templateUrl: 'activity.html', controller: ActivityCtrl}).
	when('/', {templateUrl: 'muboxmain.html', controller: FileCtrl}).
	when('/*filePath', {templateUrl: 'muboxmain.html', controller: FileCtrl})
	//.otherwise({redirectTo: '/*filePath'})
	;
}])	
//http://stackoverflow.com/questions/15731634/how-do-i-handle-right-click-events-in-angular-js
							// directive factory method
.directive('ngRightClick', function($parse) {
	// directive link function
	return function(scope, element, attrs) {
		// scope is the scope
		// element is the element where ngRightClick appears
		// attrs are all the attributes in that element, but we are interested only in attrs.ngRightClick
		var fn = $parse(attrs.ngRightClick);
		element.bind('contextmenu', function(event) {
			scope.$apply(function() {
				event.preventDefault();
				fn(scope, {$event: event});
			})
		});
	};
});

	
/* Controllers */


// function FileCtrl is defined in fileController.js
// function NotificationCtrl is defined in notificationController.js
// function VotingCtrl is defined in votingController.js

function RevisionCtrl($scope, $http, $routeParams) {
	var path = "";
	if ($routeParams.filePath) {
		path = "/" + $routeParams.filePath;
	}
	if (path) {
		$scope.revisionPath = path; 
		$http.get("../../../revisions" + path).success(function(data) {
			$scope.revisionList = data;
	   	});	
	}
}

// RevisionCtrl.$inject = ['$scope', '$http', '$routeParams'];
	
function ActivityCtrl($scope, $http, $routeParams, $timeout) {
	$scope.toggleSorting = function(name) {
		$scope.predicate = name;
		$scope.reverse = !$scope.reverse;
		var currentImg = $scope.reverse ? '/img/down.gif' : '/img/up.gif';
		var length = $scope.columnNames.length;
		for (var i = 0; i<length; i++) {
			$scope.orderImgs[$scope.columnNames[i]] = $scope.spacer;
		}
		$scope.orderImgs[name] = currentImg;
	};
	
	$scope.setUpActivityView = function(path, currentUserUid) {
		var updateActivityView = function() {
			var activityPath = '../../../activity' + path;
			if (typeof currentUserUid !== "undefined") {
				activityPath += "?uid=" + currentUserUid;
			}
			$http.get(activityPath).success(function(data) {
				$scope.activityList = data.activityList;
				$scope.userName = data.userName;
			});
			// variable updating will exist at this time
			updating = $timeout(updateActivityView, 60000);
		};
		var updating = $timeout(updateActivityView, 1);		
		// Canceling when navigating away
		$scope.$on("$destroy", function() {
			$timeout.cancel(updating)
		});
		
	};
	
	/************  ActivityCtrl Main function   ****************************/
	
	//<div ng-init='columnNames = ["filename", "action", "userName", "date", "details"]'></div>
	//<div ng-init="predicate='date'; reverse=true; orderImgs={filename:'#', action:'#', userName:'#', date:'/img/down.gif', details:'#'};"></div>
	
	$scope.spacer = 'http://upload.wikimedia.org/wikipedia/commons/5/52/Spacer.gif';
	$scope.columnNames = ["filename", "action", "userName", "date", "details"];
	$scope.predicate = 'date';
	$scope.reverse = true;
	var spacer = $scope.spacer;
	$scope.orderImgs = {filename:spacer, action:spacer, userName:spacer, date:'/img/down.gif', details:spacer };
	
	$scope.search = {filename: '', action: '', userName: '', date: '', dateFrom: '', dateTo: '', details: ''};
	if ($routeParams.username) {
		$scope.search.userName = $routeParams.username;
	}
	
	$scope.path = $routeParams.filePath ?  '/' + $routeParams.filePath : '/';
	$scope.setUpActivityView($scope.path, $routeParams.uid);

	// Wait till the angular directive executes and generates cells, then attach tooltip to those cells
	$timeout(function() {
		$('.filenamecell').tooltip();
	}, 500);
}

// ActivityCtrl.$inject = ['$scope', '$http', '$routeParams'];


/* Filters */

angular.module('activityFilters', [])
.filter('filterActivities', function() {
	return function(activityList, search) {
		if (typeof activityList === "undefined") {
			return activityList;
		}
		var out = [];
		var length = activityList.length;
		for (var i = 0; i < length; i++) {
			var activity = activityList[i];
			var activityTime = activity.date;
			var searchDateFrom = search.dateFrom;
			var searchTimeFrom = -1;
			if (typeof searchDateFrom === "object") {
				searchTimeFrom = searchDateFrom.getTime();
			}
			var searchDateTo = search.dateTo;
			var searchTimeTo = -1;
			if (typeof searchDateTo === "object") {
				searchTimeTo = searchDateTo.getTime();
			}
			if ((activity.filename.toLowerCase().indexOf(search.filename.toLowerCase()) != -1) 
					&& ((activity.action === null && search.action === '') || (activity.action !== null && activity.action.toLowerCase().indexOf(search.action.toLowerCase()) != -1))
					&& ((activity.userName === null && search.userName === '') || activity.userName !== null && activity.userName.toLowerCase().indexOf(search.userName.toLowerCase()) != -1)
					&& (searchTimeFrom === -1 || searchTimeFrom <= activityTime)
					&& (searchTimeTo === -1 || searchTimeTo >= activityTime)
					&& (activity.details.toLowerCase().indexOf(search.details.toLowerCase()) != -1) 
				) {
				out.push(activity);
			}
		}
		return out;
	};
})
.filter('formatDate', function() {
	return function(msecs) {
		var date1 = new Date(msecs);
		return date1.toLocaleDateString();
		// also can try toLocaleString(), 
	};
})
.filter('getActivityIcon', function() {
	return function(action) {
		if (action === "rename" || action === "edit") {
			return "icon-edit";
		}
		else if (action === "copy") {
			return "icon-copy";
		}
		else if (action === "newfolder") {
			return "icon-folder-close-alt";
		}
		else if (action === "upload") {
			return "icon-upload-alt";
		}
		else if (action === "delete") {
			return "icon-remove";
		}
		else if (action === "move") {
			return "icon-cut";
		}
		else if (action === "restore") {
			return "icon-undo";
		}
		else {
			return "";
		}
	};
})
;
