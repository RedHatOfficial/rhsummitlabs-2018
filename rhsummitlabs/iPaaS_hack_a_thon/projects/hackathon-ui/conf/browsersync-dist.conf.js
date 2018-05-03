const conf = require('./gulp.conf');

module.exports = function () {
  return {
    server: {
      baseDir: [
        conf.paths.dist
      ]
    },
    open: false,
    port: 8080,
    ui: {
      port: 8081
    }
  };
};
