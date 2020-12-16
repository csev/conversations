Vue.component('timeline', {
  template: `
<div class="conversations-timeline-wrapper">
    <div class="conversations-timeline" ref="timeline">
        <strong>Timeline</strong>
        <template v-if="initialPost">
            <div>{{formatEpochDate(minDate)}}</div>
            <div class="timeline-slider-rail" ref="slider_rail">
              <div class="timeline-slider" ref="slider">
                <div class="timeline-slider-annotation">
                  <template v-if="annotation">
                    {{annotation}}
                  </template>
                </div>
              </div>
            </div>
            <div>{{formatEpochDate(maxDate)}}</div>
        </template>
        <template v-else>
            <div>Loading timeline...</div>
        </template>
    </div>
</div>
`,
  data: function() {
    return {
      dragEnabled: false,
      offset: 0,
      dragging: false,
    };
  },
  props: ['posts', 'initialPost'],
  computed: {
    minDate: function() {
      return this.initialPost.postedAt;
    },
    maxDate: function() {
      if (this.posts.length === 0) {
        return this.initialPost.postedAt;
      }

      return this.posts[this.posts.length - 1].postedAt;
    },
    postsCount: function() {
        return this.posts.length + 1;
    },
    targetPostIndex: function() {
        if (this.offset == undefined) {
          return undefined;
        }
        if (this.initialPost) {
            if (this.offset === 0) {
                return 0;
            } else {
                var frac = this.offset / $(this.$refs.slider_rail).height();
                var postIndex = Math.max(Math.min(Math.round(frac * (this.postsCount)), this.postsCount), 0);
                return postIndex;
            }
        } else {
            return undefined;
        }
    },
    annotation: function() {
        if (this.targetPostIndex != undefined) {
            return "Post " + (this.targetPostIndex + 1) + " of " + this.postsCount;
        } else {
            return undefined;
        }
    },
    popupDisplayString: function() {
      if (this.targetPostIndex != undefined) {
          return (this.targetPostIndex + 1) + " / " + this.postsCount;
      } else {
          return undefined;
      }
    },
  },
  methods: {
    syncTargetPost: function(callback) {
      if (this.targetPostIndex != undefined) {
        var targetPost = undefined;
        if (this.targetPostIndex === 0) {
          targetPost = this.initialPost;
        } else {
          targetPost = this.posts[this.targetPostIndex - 1];
        }
        if (targetPost) {
          this.$parent.focusAndHighlightPost(targetPost.uuid);
          setTimeout(callback, 2000);
        }
      }
    },
    formatEpochDate: function(epoch) {
      return new Date(epoch).toLocaleDateString();
    },
    scrollToPost: function(post) {
      this.$parent.focusAndHighlightPost(post.uuid);
    },
    resize: function() {
        var height = $(window).height() - 300;
      $(this.$refs.timeline).height(height);

      var sliderRailHeight = $(this.$refs.slider_rail).height();
      var sliderHeight = 60;
    },
    resyncSlider: function() {
        $(this.$refs.slider).css('top', 0); // FIXME sync with body scrollbar
    },
    onScroll: function() {
        var maxOffset = $(this.$refs.slider_rail).height() - $(this.$refs.slider).height();

        // position at top
        if (window.scrollY > 150) {
            $(this.$el).addClass('fixed');
        } else {
            $(this.$el).removeClass('fixed');
        }

        if (!this.dragging) {
          $(this.$refs.slider).addClass('scrolling');
          setTimeout(() => {
            $(this.$refs.slider).removeClass('scrolling')
          }, 1000);
          if ((window.innerHeight + window.scrollY) >= document.body.offsetHeight) {
            this.offset = maxOffset;
            $(this.$refs.slider).css('top', this.offset + 'px');
            return;
          }

          if (window.scrollY == 0) {
              this.offset = 0;
              $(this.$refs.slider).css('top', '0px');
              return;
          }

          var $posts = $('.conversations-post', this.$parent.$el);
          for (var i=0; i<$posts.length; i++) {
            var boundProps = $posts[i].getBoundingClientRect();
            if (boundProps.y < 0 && (boundProps.y + boundProps.height) > 0) {
              if (i == 0 ) {
                this.offset = 0;
              } else {
                this.offset = (i / this.postsCount) * maxOffset;
              }
              $(this.$refs.slider).css('top', this.offset + 'px');

              return;
            }
          }
        }
    },
  },
  updated: function() {
    if (!this.dragEnabled) {
      $(this.$refs.slider).draggable({
        axis: "y",
        containment: "parent",
        start: () => {
          this.offset = undefined;
          this.dragging = true;
        },
        drag: () => {
          this.offset = parseInt($(this.$refs.slider).css('top'));
        },
        stop: () => {
          this.syncTargetPost(() => {
            this.dragging = false;
          });
        }
      });
      this.dragEnabled = true;
      this.resize();
    }
  },
  mounted: function() {
      $(window).resize(() => {
          this.resize();
          this.resyncSlider();
      });
      this.$nextTick(() => {
          this.resize();
      });

      $(window).on('scroll', () => {
          this.onScroll();
      });
  },
});
