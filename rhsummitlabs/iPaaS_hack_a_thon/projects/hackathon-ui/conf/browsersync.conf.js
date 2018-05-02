const conf = require('./gulp.conf');

module.exports = function () {
  return {
    server: {
      baseDir: [
        conf.paths.tmp,
        conf.paths.src
      ],
      routes: {
        '/bower_components': 'bower_components',
        '/node_modules': 'node_modules'
      }
    },
    codeSync: false,
    open: false,
    port: 8080,
    ui: false
  };
};
