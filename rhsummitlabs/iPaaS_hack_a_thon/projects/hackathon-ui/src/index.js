angular
  .module('app', [
    'ui.router',
    'patternfly.card',
    'patternfly.form',
    'patternfly.navigation',
    'patternfly.notification',
    'patternfly.views',
    'leaflet-directive']);

angular
  .module('app')
  .config(['NotificationsProvider', function (NotificationsProvider) {
    NotificationsProvider
      .setDelay(3000)
      .setVerbose(false)
      .setPersist({
        error: true,
        httpError: true,
        warn: true
      });
  }]);
