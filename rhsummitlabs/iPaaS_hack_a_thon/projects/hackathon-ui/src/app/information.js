angular
  .module('app')
  .component('recentInformation', {
    templateUrl: 'app/information.html'
  });

angular
  .module('app')
  .controller('InformationController', ['$scope', 'Locations', function ($scope, Locations) {
    angular.extend($scope, {
      location: {
        title: 'London',
        type: 'Restaurant',
        comments: []
      },
      listconfig: {
        selectItems: false,
        multiSelect: false,
        showSelectBox: false
      }
    });

    $scope.$watch(function () {
      return Locations.selected;
    }, function (newVal) {
      angular.extend($scope, {
        location: newVal
      });
    });
  }]);
