angular
  .module('app')
  .component('mapbox', {
    templateUrl: 'app/mapbox.html'
  });

angular
  .module('app').controller('LeafletController', ['$window', '$scope', '$log', 'Locations', function ($window, $scope, $log, Locations) {
    var default_icon = {
      iconUrl: 'bower_components/leaflet/dist/images/marker-icon.png',
      iconRetinaUrl: 'bower_components/leaflet/dist/images/marker-icon-2x.png',
      shadowUrl: 'bower_components/leaflet/dist/images/marker-shadow.png',
    };
    var moscone = {
      location: {
        lat: 37.784323,
        lng: -122.40069,
        message: 'Moscone Center',
        draggable: false,
        riseOnHover: true,
        icon: default_icon
      },
      title: 'Moscone Center',
      type: 'Point of Interest',
      comments: []
    };

    angular.extend($scope, {
      moscone: {
        lat: 37.784323,
        lng: -122.40069,
        zoom: 12
      },
      maxbounds: {},
      markers: {
        0: angular.copy(moscone.location)
      },
      events: {
        markers: {
          enable: ['click']
        }
      },
      tiles: {
        url: 'https://api.mapbox.com/v4/{mapid}/{z}/{x}/{y}.png?access_token={apikey}',
        type: 'xyz',
        options: {
          apikey: 'pk.eyJ1IjoiaGd1ZXJyZXJvbyIsImEiOiJjamdjbWx2Nm0xazl2MndvNDhiYmZ5MDI0In0.AdSTbfPP_0C0u5rkjDD7AA',
          mapid: 'mapbox.streets'
        }
      },
      defaults: {
        scrollWheelZoom: false
      }
    });

    Locations.locations.push(moscone);

    $scope.$on('leafletDirectiveMarker.canvasMap.click', function (event, args) {
      var location = Locations.locations[args.modelName];
      if (angular.isUndefined(location.comments)) {
        location.comments = [];
      }
      Locations.selected = location;
      $log.info('marker:' + Locations.selected.title);
    });

    var connection = $window.connection;

    initAMQ(connection);

    function initAMQ(connection) {
      var receiver = connection.open_receiver(config.mq_locations);

      receiver.on('message', function (context) {
        $log.debug('Raw message: ' + context.message);

        var location = angular.fromJson(context.message.body ? context.message.body : context.message);

        $log.info('Message received: ' + angular.toJson(location));

        Locations.locations = location;

        var mapLocations = Locations.locations.map(a => {
          return {
            lat: a.location.lat,
            lng: a.location.lng,
            title: a.title,
            message: a.title,
            draggable: false,
            icon: {
              iconUrl: 'bower_components/leaflet/dist/images/marker-icon.png',
              iconRetinaUrl: 'bower_components/leaflet/dist/images/marker-icon-2x.png',
              shadowUrl: 'bower_components/leaflet/dist/images/marker-shadow.png',
            }
          };
        });
        angular.extend($scope, {
          markers: mapLocations
        });
        $scope.$apply();
      });
    }
  }]);
