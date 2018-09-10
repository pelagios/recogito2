define([], function() {

  var getMessage = function(unannotated, annotated, quote) {
        var message;

        if (unannotated + annotated != 0) {
          if (annotated == 0) {
            if (unannotated == 1)
              message = 'There is 1 more un-annotated occurrence';
            else
              message = 'There are ' + unannotated + ' more un-annotated occurrences';
          } else if (unannotated == 0) {
            if (unannotated == 1)
              message = 'There is 1 other annotated occurrence';
            else
              message = 'There are ' + annotated + ' other annotated occurrences';
          } else {
            if (annotated == 1 && unannotated == 1)
              message = 'There is 1 <em>un-annotated</em> and 1 <em>annotated</em> occurrence';
            else
              message = 'There are ' + unannotated + ' <em>un-annotated</em> and ' + annotated + ' <em>annotated</em> occurrences';
          }

          message += ' of <span class="quote">' + quote + '</span> in the text. Do you want to re-apply this annotation?';
        }

        return message;
      },

      getButtons = function(annotated) {
        if (annotated > 0)
          return '<span class="stacked">' +
            '<button class="btn" data-action="REPLACE">YES, replace existing annotations</button>' +
            '<button class="btn" data-action="MERGE">YES, merge with existing annotations</button>' +
            '<button class="btn outline" data-action="CANCEL">NO, don\'t re-apply</button>' +
          '</span>';
        else
          return '<span>' +
            '<button class="btn" data-action="REPLACE">YES</button>' +
            '<button class="btn outline" data-action="CANCEL">NO</button>' +
          '</span>';
      };

  return {

    prompt : function(quote, unannotatedCount, annotated, actions) {
      var message = getMessage(unannotatedCount, annotated.length, quote),
          buttons = getButtons(annotated.length),
          element = jQuery(
            '<div class="clicktrap">' +
              '<div class="alert info reapply">' +
                '<h1>Re-Apply</h1>' +
                '<p class="message">' + message + '</p>' +
                '<p class="buttons">' + buttons + '</div>' +
              '</div>' +
            '</div>').appendTo(document.body);

      element.on('click', 'button', function(evt) {
        var btn = jQuery(evt.target),
            action = btn.data('action');

        actions[action]();

        element.remove();
      });
    }

  };

});
