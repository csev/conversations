Vue.component('topic-settings-form', {
  template: `
  <div class="row topic-settings-form">
    <div class="col-sm-6 col-sm-offset-2" style="border-right: 1px solid #EEE;">
      <h3 class="center-text">Choose Your Settings:</h3>
      <div class="conversations-settings-section">
        <div class="row">
          <div class="col-sm-8"><label for="published">Published to Students</label></div>
          <div class="col-sm-4 text-right">
            <input id="published" type="checkbox" name="published" v-model="settings.published"/>
          </div>
        </div>
        <template v-if="settings.published">
          <br>
          <div class="row">
            <div class="col-sm-12">Who has access</div>
          </div>
          <div class="row">
            <div class="col-sm-12"><label><input type="radio" name="availability" value="ENTIRE_SITE" v-model="settings.availability"/> Available to <strong>entite site</strong></label></div>
          </div>
          <div class="row">
            <div class="col-sm-12">
              <label><input type="radio" name="availability" value="GROUPS" v-model="settings.availability"/> Available to <strong>select groups</strong></label>
            </div>
            <template v-if="settings.availability === 'GROUPS'">
              <div style="margin-left: 40px">
                <template v-if="sections.length > 0">
                  <div><strong>Sections</strong></div>
                  <div v-for="section in sections">
                    <label>
                      <input type="checkbox"
                             name="group[]"
                             v-bind:value="section.reference"
                             v-model="settings.groups"/> {{section.name}}
                    </label>
                  </div>
                </template>
                <template v-if="groups.length > 0">
                  <div><strong>Groups</strong></div>
                  <div v-for="group in groups">
                    <label>
                      <input type="checkbox"
                             name="group[]"
                             v-bind:value="group.reference"
                             v-model="settings.groups"/> {{group.name}}
                    </label>
                  </div>
                </template>
              </div>
            </template>
          </div>
        </template>
      </div>
      <br>
      <div class="conversations-settings-section">
        <div class="row">
          <div class="col-sm-8"><label for="graded">Graded Topic</label></div>
          <div class="col-sm-4 text-right">
            <input id="graded" type="checkbox" name="graded" v-model="settings.graded"/>
          </div>
        </div>
        <template v-if="settings.graded">
          <div class="row">
            <div class="col-sm-12"><p style="font-style: italic;">Coming soon.</p></div>
          </div>
        </template>
      </div>
    </div>
    <div class="col-sm-4" >
      <h3 class="center-text">Additional Options:</h3>
      <div class="row">
        <div class="col-sm-12">
          <label><input type="checkbox" name="allow_comments" v-model="settings.allow_comments"/> Allow Comments</label>
        </div>
      </div>
      <div class="row">
        <div class="col-sm-12">
          <label><input type="checkbox" name="allow_like" v-model="settings.allow_like"/> Likes</label>
        </div>
      </div>
      <div class="row">
        <div class="col-sm-12">
          <label><input type="checkbox" name="require_post" v-model="settings.require_post"/> Require post before showing others' posts</label>
        </div>
      </div>
    </div>
  </div>
`,
  props: ['settings', 'available_groups'],
  computed: {
    sections: function() {
      var result = [];

      this.available_groups.forEach(function(group) {
        if (group.type == 'section') {
          result.push(group);
        }
      });

      return result;
    },
    groups: function() {
      var result = [];

      this.available_groups.forEach(function(group) {
        if (group.type == 'group') {
          result.push(group);
        }
      });

      return result;
    },
  },
});

Vue.component('create-topic-workflow', {
  template: `
  <div class="conversations-create-topic-workflow">
    <template v-if="step == 'SELECT_TYPE'">
      <div class="row">
        <div class="col-sm-12">
          <p class="text-center">Choose your topic type:</p>
        </div>
      </div>
      <div class="row">
        <div class="col-sm-3">
          <div class="conversations-topic">
            <div class="conversations-topic-banner react"></div>
            <div class="conversations-topic-content">
                <p><strong>React</strong></p>
                <p>Allows students to respond to an initial prompt.</p>
                <p><button class="button" v-on:click="selectTopicType('react')">Select Topic Type</button></p>
            </div>
          </div>
        </div>
        <div class="col-sm-3">
          <div class="conversations-topic">
            <div class="conversations-topic-banner brainstorm"></div>
            <div class="conversations-topic-content">
                <p><strong>Brainstorm</strong></p>
                <p style="font-style: italic;">Coming soon.</p>
            </div>
          </div>
        </div>
        <div class="col-sm-3">
          <div class="conversations-topic">
            <div class="conversations-topic-banner discuss"></div>
            <div class="conversations-topic-content">
                <p><strong>Discuss</strong></p>
                <p style="font-style: italic;">Coming soon.</p>
            </div>
          </div>
        </div>
      </div>
    </template> 
    <template v-else-if="step == 'SET_TITLE'">
      <div class="row">
        <div class="col-sm-12">
          <i class="fa fa-arrow-left" aria-hidden="true"></i> <a href="#" v-on:click="step = 'SELECT_TYPE'">Back to select type</a>
        </div>
      </div>
      <div class="row">
        <div class="col-sm-6 col-sm-offset-3">
          <p class="text-center">
            <input class="form-control" placeholder="Topic title" v-model="topicTitle">
          </p>
        </div>
      </div>
      <div class="row">
        <div class="col-sm-12">
          <p class="text-center">
            <button class="button" v-on:click="selectTopicTitle()">Next <i class="fa fa-arrow-right" aria-hidden="true"></i></button>
          </p>
        </div>
      </div>
    </template>
    <template v-else-if="step == 'TOPIC_SETTINGS'">
      <div class="row">
        <div class="col-sm-12">
          <i class="fa fa-arrow-left" aria-hidden="true"></i> <a href="#" v-on:click="step = 'SET_TITLE'">Back to topic title</a>
        </div>
      </div>
      <topic-settings-form :settings="settings" :available_groups="available_groups"></topic-settings-form>
      <br>
      <div class="row">
        <div class="col-sm-12">
          <p class="text-center">
            <button class="button" v-on:click="setTopicSettings()">Next <i class="fa fa-arrow-right" aria-hidden="true"></i></button>
          </p>
        </div>
      </div>
    </template>
    <template v-else-if="step == 'CREATE_FIRST_POST'">
      <div class="row">
        <div class="col-sm-12">
          <i class="fa fa-arrow-left" aria-hidden="true"></i> <a href="#" v-on:click="step = 'SET_TITLE'">Back to set title</a>
        </div>
      </div>
      <div class="row">
        <div class="col-sm-6 col-sm-offset-3">
          <p class="text-center">
            <div class="post-to-topic-textarea form-control">
              <div class="stretchy-editor" v-bind:class='{ "full-editor-height": editorFocused }'>
                <div class="topic-ckeditor"></div>
              </div>
            </div>
          </p>
        </div>
      </div>
      <div class="row">
        <div class="col-sm-12">
          <p class="text-center">
            <button class="button_color" v-on:click="createTopic()"><i class="fa fa-plus" aria-hidden="true"></i> Create Post</button>
          </p>
        </div>
      </div>
    </template>
  </div>
`,
  data: function() {
    return {
      step: 'SELECT_TYPE',
      topicType: null,
      topicTitle: '',
      editorFocused: false,
      settings: {
        published: false,
        availability: 'ENTIRE_SITE',
        graded: false,
        allow_comments: true,
        allow_like: false,
        require_post: false,
        groups: [],
      },
    };
  },
  props: ['baseurl', 'available_groups'],
  methods: {
    firstPostContent: function() {
      if (this.editor) {
        return this.editor.getData();
      } else {
        return '';
      }
    },
    selectTopicType: function(type) {
      this.topicType = type;
      this.step = 'SET_TITLE';
    },
    selectTopicTitle: function() {
      if (this.topicTitle != '') {
        this.step = 'TOPIC_SETTINGS';
      }
    },
    setTopicSettings: function() {
      this.step = 'CREATE_FIRST_POST';
    },
    createTopic: function() {
      if (this.firstPostContent() != '') {
        $.ajax({
          url: this.baseurl + 'create-topic',
          method: 'post',
          data: {
            title: this.topicTitle,
            type: this.topicType,
            settings: this.settings,
            post: this.firstPostContent(),
          },
          success: function() {
            location.reload();
          },
          error: (jqXHR, textStatus, errorThrown) => {
            var $error = $('<div class="alertMessage">').text(jqXHR.responseText);
            $(this.$el).find('.modal-body').prepend($error);
          }
        });
      }
    },
    initRichTextareas: function() {
      $(this.$el).find('.topic-ckeditor').each((idx, elt) => {
        RichText.initialize({
          baseurl: this.baseurl,
          elt: elt,
          placeholder: 'Add initial topic post content...',
          onCreate: (newEditor) => {
            this.editor = newEditor;
          },
          onFocus: (event, name, isFocused) => {
            if (isFocused) {
              this.editorFocused = isFocused;
            }
          },
        });
      });
    },
  },
  updated: function() {
    this.initRichTextareas();
  },
});

Vue.component('create-topic-modal', {
  template: `
  <div class="conversations-modal conversations-create-topic-modal">
    <div class="modal" ref="dialog" role="dialog">
      <div class="modal-dialog" role="document">
        <div class="modal-content">
          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
              <span aria-hidden="true">&times;</span>
            </button>
            <div class="text-center">
              <span class="modal-title text-center">Add New Topic</span>
            </div>
          </div>
          <div class="modal-body">
            <create-topic-workflow :baseurl="baseurl" :available_groups="available_groups"></create-topic-workflow>
          </div>
        </div>
      </div>
    </div>
  </div>
`,
  data: function() {
    return {};
  },
  props: ['baseurl', 'available_groups'],
  methods: {
    show: function() {
      $(this.$refs.dialog).modal();
      this.resize();
    },
    resize: function() {
      const $dialog = $(this.$refs.dialog);
      if ($dialog.find('.modal-dialog').is(':visible')) {
        $dialog.find('.modal-dialog').width('95%');
        $dialog.find('.modal-content').height($(window).height() - 70);
      }
    },
  },
  mounted: function() {
    $(window).resize(() => {
      this.resize();
    });

    const $dialog = $(this.$refs.dialog);
    $dialog.on('shown.bs.modal', function() {
      $(document.body).css('overflow', 'hidden');
    }).on('hidden.bs.modal', function() {
      $(document.body).css('overflow', '');
    });
  },
});

Vue.component('create-topic-wrapper', {
  template: `
  <div class="conversations-create-topic-wrapper">
    <button class="button" v-on:click="showModal()"><i class="fa fa-plus" aria-hidden="true"></i> New Topic</button>
    <create-topic-modal ref="createTopicModal"
                        :baseurl="baseurl"
                        :available_groups="available_groups">
    </create-topic-modal>
  </div>
`,
  data: function() {
    return {
        available_groups: JSON.parse(this.groups_json),
    };
  },
  props: ['baseurl', 'groups_json'],
  methods: {
    showModal: function() {
      this.$refs.createTopicModal.show();
    },
  },
  mounted: function() {
  },
});

Vue.component('update-topic-settings-modal', {
  template: `
  <div class="conversations-modal conversations-update-topic-modal">
    <div class="modal" ref="dialog" role="dialog">
      <div class="modal-dialog" role="document">
        <div class="modal-content">
          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
              <span aria-hidden="true">&times;</span>
            </button>
            <div class="text-center">
              <span class="modal-title text-center">Edit Topic Settings</span>
            </div>
          </div>
          <div class="modal-body">
            <template v-if="topic != null">
              <topic-settings-form :settings="topic.settings"
                                   :available_groups="available_groups">
              </topic-settings-form>
            </template>
            <div class="row">
              <div class="col-sm-12">
                <br>
                <p class="text-center">
                  <button class="button" @click="saveSettings()">Save Settings</button>
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
`,
  data: function() {
    return {
        topic: null,
        available_groups: [],
    };
  },
  props: ['baseurl', 'topic_uuid'],
  methods: {
    show: function() {
      $(document.body).append(this.$el);
      this.fetchTopic();
      $(this.$refs.dialog).modal();
      this.resize();
    },
    resize: function() {
      const $dialog = $(this.$refs.dialog);
      if ($dialog.find('.modal-dialog').is(':visible')) {
        $dialog.find('.modal-dialog').width('95%');
        $dialog.find('.modal-content').height($(window).height() - 70);
      }
    },
    saveSettings: function() {
      $.ajax({
        url: this.baseurl + 'update-topic',
        method: 'post',
        data: {
          uuid: this.topic_uuid,
          settings: this.topic.settings,
        },
        success: function() {
          location.reload();
        },
        error: (jqXHR, textStatus, errorThrown) => {
          var $error = $('<div class="alertMessage">').text(jqXHR.responseText);
          $(this.$el).find('.modal-body').prepend($error);
        }
      });
    },
    fetchTopic: function() {
      $.ajax({
        url: this.baseurl+'feed/topic',
        type: 'get',
        data: {topicUuid: this.topic_uuid},
        dataType: 'json',
        success: (json) => {
           this.topic = json.topic;
           this.available_groups = json.available_groups;
        },
        error: (jqXHR, textStatus, errorThrown) => {
          var $error = $('<div class="alertMessage">').text(jqXHR.responseText);
          $(this.$el).find('.modal-body').prepend($error);
        }
      });
    },
  },
  mounted: function() {
    $(window).resize(() => {
      this.resize();
    });

    const $dialog = $(this.$refs.dialog);
    $dialog.on('shown.bs.modal', function() {
      $(document.body).css('overflow', 'hidden');
    }).on('hidden.bs.modal', function() {
      $(document.body).css('overflow', '');
    });
  },
});

Vue.component('edit-topic-settings-wrapper', {
  template: `
  <span>
    <a href="javascript:void(0)" @click.stop.prevent="showModal()" title="Edit Topic" class="button"><i class="fa fa-pencil"></i></a>
    <update-topic-settings-modal ref="updateTopicSettingsModal" :baseurl="baseurl" :topic_uuid="topic.uuid"></update-topic-settings-modal>
  </div>
`,
  data: function() {
    return {};
  },
  props: ['baseurl', 'topic'],
  methods: {
    showModal: function() {
      this.$refs.updateTopicSettingsModal.show();
    },
  },
  mounted: function() {
  },
});