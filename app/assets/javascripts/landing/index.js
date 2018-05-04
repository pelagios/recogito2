require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([], function() {

  jQuery(document).ready(function() {
    var widget = jQuery('.recogito-rightnow'),

        annotationsEl = widget.find('.annotations h3'),
        editsEl = widget.find('.edits h3'),
        usersEl = widget.find('.users h3'),

        format = function(num) {
          if (num > 10000) {
            return Math.round(num / 1000) + 'K';
          } else if (num > 1000) {
            var t = Math.floor(num / 1000);
            var h = num % 1000;
            if (h > 99)
              return t + ',' + h;
            else if (h > 9)
              return t + ',0' + h;
            else
              return t + ',00' + h;
          } else {
            return num;
          }
        },

        refreshStats = function() {
          jsRoutes.controllers.landing.LandingController.getStats().ajax()
            .done(function(response) {
              annotationsEl.html(format(response.annotations));
              editsEl.html(format(response.edits));
              usersEl.html(format(response.users));

              window.setTimeout(function() { refreshStats(); }, 5000);
            });
        },

        showCookiePopup = function() {
          window.cookieconsent.initialise({
            palette: {
              popup  : { background: '#252e39' },
              button : { background: '#4483c4' }
            },
            position: 'bottom-left',
            content: {
              message: 'Recogito uses cookies only for the purpose of login. Cookies are ' +
                'removed when you log out.',
              href: '/help/about#cookies'
            }
          });
        };

    // Activate the carousel
    jQuery('.testimonials .inner').slick({
      autoplay      : true,
      autoplaySpeed : 12000,
      pauseOnHover  : true
    });

    showCookiePopup();

    // Start stats poll loop
    refreshStats();
  });

});
