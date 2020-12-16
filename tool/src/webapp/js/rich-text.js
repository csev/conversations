(function() {
  class UploadAdapter {
    constructor(loader, baseurl, callbacks) {
      this.loader = loader;
      this.baseurl = baseurl;
      this.callbacks = callbacks;
    }

    upload() {
      return this.loader.file.then((file) =>
        new Promise((resolve, reject) => {
          this.handleUpload(file, resolve, reject);
        }));
    }

    abort() {
    }

    handleUpload(file, resolve, reject) {
      const self = this;

      if (self.callbacks.uploadStarted) {
        self.callbacks.uploadStarted();
      }

      const formData = new FormData();
      formData.append('file', file);
      formData.append('mode', 'inline-upload');

      $.ajax({
        url: self.baseurl + 'file-upload',
        type: 'POST',
        contentType: false,
        cache: false,
        processData: false,
        data: formData,
        dataType: 'json',
        success: function(response) {
          resolve({
            default: self.baseurl + 'file-view?mode=view&key=' + response.key,
          });
        },
        error: function(xhr, statusText) {
          reject(statusText);
        },
        complete: function() {
          setTimeout(function() {
            if (self.callbacks.uploadFinished) {
              self.callbacks.uploadFinished();
            }
          }, 0);
        },
      });
    }
  }


  const initialize = function(opts) {
    if ($(opts.elt).hasClass('rich-text-initialized')) {
      return;
    }

    InlineEditor
        .create(opts.elt, {
          placeholder: (opts.placeholder || 'Type something'),
        })
        .then((newEditor) => {
          newEditor.activeUploads = 0;
          newEditor.plugins.get('FileRepository').createUploadAdapter = (loader) => {
            return new UploadAdapter(loader, opts.baseurl,
                {
                  uploadStarted: () => {
                    opts.onUploadEvent && opts.onUploadEvent('started');
                  },
                  uploadFinished: () => {
                    opts.onUploadEvent && opts.onUploadEvent('finished');
                  },
                });
          };

          newEditor.ui.focusTracker.on('change:isFocused', (event, name, isFocused) => {
            opts.onFocus(event, name, isFocused);
          });

          opts.onCreate(newEditor);
        })
        .catch(function(error) {
          console.error(error);
        });

    $(opts.elt).addClass('rich-text-initialized');
  };

  window.RichText = {};
  window.RichText.initialize = initialize;
}());
