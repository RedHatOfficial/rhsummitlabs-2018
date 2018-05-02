angular
  .module('app')
  .component('locationDetails', {
    templateUrl: 'app/location.html'
  });

angular
  .module('app')
  .controller('LocationDetailsController', ['$scope', 'Locations', function ($scope, Locations) {
    $scope.$watch(function () {
      return Locations.selected;
    }, function (newVal) {
      angular.extend($scope, {
        site: newVal
      });
    });
  }]);
