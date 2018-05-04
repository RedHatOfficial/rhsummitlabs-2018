angular
  .module('app')
  .service('Locations', [function () {
    var selected;
    var locations;

    selected = {
      title: '',
      location: {},
      comments: []
    };

    locations = [];

    return {
      selected: selected,
      locations: locations
    };
  }]);
