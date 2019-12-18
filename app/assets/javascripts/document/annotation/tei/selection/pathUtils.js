define([], function() {

  return {

    // https://www.benpickles.com/articles/51-finding-a-dom-nodes-common-ancestor-using-javascript
    getCommonAncestor : function(node1, node2) {
      var parents = function(node) {
            var nodes = [ node ];
            for (; node; node = node.parentNode) {
              nodes.unshift(node);
            }
            return nodes;
          },

          parents1 = parents(node1),
          parents2 = parents(node2);

      // No common ancestor
      if (parents1[0] !== parents2[0]) return;

      for (var i=0; i<parents1.length; i++) {
        if (parents1[i] !== parents2[i]) return parents1[i - 1];
      }
    },

    getXPath : function(node, path) {
      var count, xpath, predicate;

      path = path || [];

      if (node.nodeType == Node.ELEMENT_NODE && node.hasAttribute("xml:id")) {
        path.push("/");
      } else if (node.parentNode) { // && node.parentNode.nodeName.toLowerCase().startsWith("tei-")) {
        path = this.getXPath(node.parentNode, path);
      }

      if (node.nodeType == Node.ELEMENT_NODE && node.nodeName.toLowerCase().startsWith("tei-")) {
        xpath = "count(preceding-sibling::"+node.localName+")";
        count = document.evaluate(xpath, node, null, XPathResult.NUMBER, null).numberValue + 1;
        if (node.hasAttribute("xml:id")) {
          predicate = "[@xml:id='"+node.getAttribute("xml:id")+"']";
        } else {
          predicate = "[" + count + "]";
        }
        path.push("/");
        path.push(node.getAttribute("data-origname") + predicate);
      }

      return path;
    },

    /**
     * Helper that transforms the CETEIcean-specific DOM XPath to a
     * normalized XPath with the TEI document only.
     */
    toTEIPaths : function(rootNode, startPath, endPath, selectedRange) {
      var pathStart = rootNode.nodeName.toLowerCase() + "[@id='" + rootNode.id + "']",

          // For a given node, returns the closest parent that is a TEI element
          getClosestTEINode = function(node) {
            if (node.nodeName.toLowerCase().indexOf('tei-') === 0)
              return node;
            else
              return getClosestTEINode(node.parentNode);
          },

          // Helper to compute char offsets between end of XPath and given selection bound
          getOffsetFromTo = function(fromNode, toNode, toOffset) {
            var range = rangy.createRange();
            range.setStart(fromNode, 0);
            range.setEnd(toNode, toOffset);
            return range.toString().length;
          },

          startOffset = getOffsetFromTo(
            getClosestTEINode(selectedRange.startContainer),
            selectedRange.startContainer,
            selectedRange.startOffset),

          endOffset = getOffsetFromTo(
            getClosestTEINode(selectedRange.endContainer),
            selectedRange.endContainer,
            selectedRange.endOffset),

          startTEIPath = startPath.join('') + '::' + startOffset,
          endTEIPath = endPath.join('') + '::' + endOffset;

      return 'from=' + startTEIPath + ';to=' + endTEIPath;
    }

  };

});
