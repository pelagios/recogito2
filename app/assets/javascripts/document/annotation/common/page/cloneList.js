define([], function() {

  var CloneList = function(containerEl, clones) {

    var self = this,

        element = jQuery(
          '<div class="clone-list">' +
            '<ul>' +
            '</ul>' +
            '<div class="hint">' + 
              '<a target="_blank" href="/help/cloning">What is cloning?</a>' +
            '</div>' +
          '</div>').hide(),

        toggle = function() {
          if (element.is(':visible'))
            element.hide();
          else 
            element.show();
        },

        init = function() {
          var ul = element.find('ul');

          clones.forEach(function(clone) {
            ul.append(jQuery(
              '<li><a target="_blank" href="/document/' + clone.id + '">' + 
                clone.owner + '/' + clone.id + '</a></li>'
            ));
          }); 

          jQuery(containerEl).append(element);      
        };

    init();
    this.toggle = toggle;
  };

  return CloneList;

});