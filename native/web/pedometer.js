// <pedometer> on the web: browsers do not count steps, so every use is unavailable.
mergeInto(LibraryManager.library, {
  pedometer_start: function() { Module.uiEmit('pedometer', 'unavailable'); },
  pedometer_stop: function() {},
  pedometer_query: function(id, from, to) { Module.uiEmit('pedometer', `query ${id} unavailable`); },
  pedometer_sample: function() {},
});
