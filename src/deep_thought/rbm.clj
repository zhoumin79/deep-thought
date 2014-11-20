(ns logreg.core)
class RBM(object):

;; DEF 
;;  def __init__(self, input=None, n_visible=784, n_hidden=500,
;; W=None, hbias=None, vbias=None, numpy_rng=None, theano_rng=None):
;;                """
;;         RBM constructor. Defines the parameters of the model along with
;;         basic operations for inferring hidden from visible (and vice-versa),
;;         as well as for performing CD updates.
;;         :param input: None for standalone RBMs or symbolic variable if RBM is
;;         part of a larger graph.
;;         :param n_visible: number of visible units
;;         :param n_hidden: number of hidden units
;;         :param W: None for standalone RBMs or symbolic variable pointing to a
;;         shared weight matrix in case RBM is part of a DBN network; in a DBN,
;;         the weights are shared between RBMs and layers of a MLP
;;         :param hbias: None for standalone RBMs or symbolic variable pointing
;;         to a shared hidden units bias vector in case RBM is part of a
;;         different network
;;         :param vbias: None for standalone RBMs or a symbolic variable
;;         pointing to a shared visible units bias
;;         """

;;         self.n_visible = n_visible
;;         self.n_hidden = n_hidden
;;         if numpy_rng is None:
;;             # create a number generator
;;             numpy_rng = numpy.random.RandomState(1234)
;;         if theano_rng is None:
;;             theano_rng = RandomStreams(numpy_rng.randint(2 ** 30))
;;         if W is None:
;;             # W is initialized with `initial_W` which is uniformely
;;             # sampled from -4*sqrt(6./(n_visible+n_hidden)) and
;;             # 4*sqrt(6./(n_hidden+n_visible)) the output of uniform if
;;             # converted using asarray to dtype theano.config.floatX so
;;             # that the code is runable on GPU
;;             initial_W = numpy.asarray(
;;                 numpy_rng.uniform(
;;                     low=-4 * numpy.sqrt(6. / (n_hidden + n_visible)),
;;                     high=4 * numpy.sqrt(6. / (n_hidden + n_visible)),
;;                     size=(n_visible, n_hidden)
;;                 ),
;;                 dtype=theano.config.floatX
;;             )
;;             # theano shared variables for weights and biases
;;             W = theano.shared(value=initial_W, name='W', borrow=True)
;;         if hbias is None:
;;             # create shared variable for hidden units bias
;;             hbias = theano.shared(
;;                 value=numpy.zeros(
;;                     n_hidden,
;;                     dtype=theano.config.floatX
;;                 ),
;;                 name='hbias',
;;                 borrow=True
;;             )
;;         if vbias is None:
;;             # create shared variable for visible units bias
;;             vbias = theano.shared(
;;                 value=numpy.zeros(
;;                     n_visible,
;;                     dtype=theano.config.floatX
;;                 ),
;;                 name='vbias',
;;                 borrow=True
;;             )
;;         # initialize input layer for standalone RBM or layer0 of DBN
;;         self.input = input
;;         if not input:
;;             self.input = T.matrix('input')
;;         self.W = W
;;         self.hbias = hbias
;;         self.vbias = vbias
;;         self.theano_rng = theano_rng
;;        # **** WARNING: It is not a good idea to put things in this list
;;         # other than shared variables created in this function.
;;         self.params = [self.W, self.hbias, self.vbias]

;; DEF 
;; def propup(self, vis):
(defn propup
  "This function propagates the visible units activation upwards to
  the hidden units
  
  Note that we return also the pre-sigmoid activation of the
  layer. As it will turn out later, due to how Theano deals with
  optimizations, this symbolic variable will be needed to write
  down a more stable computational graph (see details in the
  reconstruction cost function)"
  [vis W hbias]
  (let [pre-sigmoid-activation (+ ((* vis W) hbias))]
    [pre-sigmoid-activation, (sigmoid pre-sigmoid-activation)]))

;; DEF 
;; def sample_h_given_v(self, v0_sample):
(defn sample-h-given-v
  "This function infers state of hidden units given visible units"
  [v0-sample]
  (let [
        [pre-sigmoid-h1 h1-mean] (propup v0-sample)
         h1-sample (binomial-sample  ; h1_sample = self.theano_rng.binomial(
                    (shape h1-mean)  ; size=v1_mean.shape,
                    1                ; n=1,
                    h1-mean)         ; p=v1_mean, dtype=theano.config.floatX)
         ]
    [pre-sigmoid-h1 h1-mean h1-sample]))

;; DEF 
;; def propdown(self, hid):
(defn propdown
  "This function propagates the hidden units activation downwards to
  the visible units

  Note that we return also the pre_sigmoid_activation of the
  layer. As it will turn out later, due to how Theano deals with
  optimizations, this symbolic variable will be needed to write
  down a more stable computational graph (see details in the
  reconstruction cost function)"
  [hid W vbias]
  (let [pre-sigmoid-activation (+ vbias (* hid W))]
    [pre-sigmoid-activation (sigmoid pre-sigmoid-activation)])
  )

        pre_sigmoid_activation = T.dot(hid, self.W.T) + self.vbias
        return [pre_sigmoid_activation, T.nnet.sigmoid(pre_sigmoid_activation)]
;; DEF
;; def sample_v_given_h(self, h0_sample):
(defn sample-v-given-h
  "This function infers state of visible units given hidden units"
  [h0-sample]
  (let [ [pre-sigmoid-v1 v1-mean] (propdown h0-sample)
         v1-sample (binomial-sample  ; v1_sample = self.theano_rng.binomial(
                    (shape v1-mean)  ; size=v1_mean.shape, 
                    1                ; n=1, 
                    v1-mean)         ; p=v1_mean, dtype=theano.config.floatX)
         ]
    [pre-sigmoid-v1 v1-mean v1-sample]))

;; DEF 
;; def gibbs_hvh(self, h0_sample):
(defn gibbs-hvh
  "This function implements one step of Gibbs sampling,
   starting from the hidden state"
  [h0-sample]
  (let [
        [pre-sigmoid-v1 v1-mean v1-sample] (sample-v-given-h h0-sample)
        [pre-sigmoid-h1 h1-mean h1-sample] (sample-h-given-v v1-sample)
        ]
    [pre-sigmoid-v1, v1-mean, v1-sample,
     pre-sigmoid-h1, h1-mean, h1-sample]))

;; DEF 
;; def gibbs_vhv(self, v0_sample):
(defn gibbs-vhv
  "This function implements one step of Gibbs sampling,
   starting from the visible state"
  [v0-sample]
  (let [
        [pre-sigmoid-h1 h1-mean h1-sample] (sample-h-given-v v0-sample)
        [pre-sigmoid-v1 v1-mean v1-sample] (sample-v-given-h h1-sample)
        
        ]
    [pre-sigmoid-h1, h1-mean, h1-sample,
     pre-sigmoid-v1, v1-mean, v1-sample]))

;; DEF 
(defn free_energy
  [v-sample W hbias vbias]
  (let [wx-b (+ (* v-sample W)
                hbias) ; wx_b = T.dot(v_sample, self.W) + self.hbias
        vbias-term (* v-sample vbias) ; vbias_term = T.dot(v_sample, self.vbias)
        hidden-term (sum (log (+ (exp wx-b) 1))) ; hidden_term = T.sum(T.log(1 + T.exp(wx_b)), axis=1)
        ]
    (- vbias-term -hidden-term)))

;; DEF 
;; def get_cost_updates(self, lr=0.1, persistent=None, k=1):
(defn get-cost-updates
  "This functions implements one step of CD-k or PCD-k
  
  :param lr: learning rate used to train the RBM

  :param persistent: None for CD. For PCD, shared variable
  containing old state of Gibbs chain. This must be a shared
  variable of size (batch size, number of hidden units).
  
  :param k: number of Gibbs steps to do in CD-k/PCD-k

  Returns a proxy for the cost and the updates dictionary. The
  dictionary contains the update rules for weights and biases but
  also an update of the shared variable used to store the persistent
  chain, if one is used."
  [lr persistent k input params]
  (let [
        [pre-sigmoid-ph ph-mean ph-sample] (sample_h_given_v input)
        chain-start (if (= null persistent) ph-sample persistent)
        [pre-sigmoid-nvs nv-means nv-samples pre-sigmoid-nhs nh-means nh-samples]
        (scan
         gibbs_hvh
         [null null null null null chain-start]
         k)
        chain-end (last nv-samples)
        cost (- (mean (free-energy chain-end))) (mean (free-energy input))
        gparams (grad cost params [chain-end])
        ]
;;      # constructs the update dictionary
;;      for gparam, param in zip(gparams, self.params):
;;          # make sure that the learning rate is of the right dtype
;;          updates[param] = param - gparam * T.cast(
;;              lr,
;;              dtype=theano.config.floatX
;;          )
;;      if persistent:
;;          # Note that this works only if persistent is a shared variable
;;          updates[persistent] = nh_samples[-1]
;;          # pseudo-likelihood is a better proxy for PCD
;;          monitoring_cost = self.get_pseudo_likelihood_cost(updates)
;;      else:
;;          # reconstruction cross-entropy is a better proxy for CD
;;          monitoring_cost = self.get_reconstruction_cost(updates,
;;                                                         pre_sigmoid_nvs[-1])
;;      return monitoring_cost, updates
    ))

;; # perform actual negative phase
;; # in order to implement CD-k/PCD-k we need to scan over the
;; # function that implements one gibbs step k times.
;; # Read Theano tutorial on scan for more information :
;; # http://deeplearning.net/software/theano/library/scan.html
;; # the scan will return the entire Gibbs chain
;; (
;;     [
;;         pre_sigmoid_nvs,
;;         nv_means,
;;         nv_samples,
;;         pre_sigmoid_nhs,
;;         nh_means,
;;         nh_samples
;;     ],
;;     updates
;; ) = theano.scan(
;;     self.gibbs_hvh,
;;     # the None are place holders, saying that
;;     # chain_start is the initial state corresponding to the
;;     # 6th output
;;     outputs_info=[None, None, None, None, None, chain_start],
;;     n_steps=k
;; )

;; # determine gradients on RBM parameters
;; # note that we only need the sample at the end of the chain
;; chain_end = nv_samples[-1]

;; cost = T.mean(self.free_energy(self.input)) - T.mean(
;;     self.free_energy(chain_end))
;; # We must not compute the gradient through the gibbs sampling
;; gparams = T.grad(cost, self.params, consider_constant=[chain_end])

;; DEF 
;; def get_pseudo_likelihood_cost(self, updates):
; Theanos stuff I don't follow
; bit_i_idx = theano.shared(value=0, name='bit_i_idx')
; updates[bit_i_idx] = (bit_i_idx + 1) % self.n_visible
(defn get-pseudo-likelihood-cost
  [input n-visible updates]
  (let
      [xi (round input) ; T.round(self.input)
       fe-xi (free-energy xi)
       xi-flip (tensor-flip xi) ; xi_flip = T.set_subtensor(xi[:, bit_i_idx], 1 - xi[:, bit_i_idx])
       fe-xi-flip (free-energy xi-flip)
       cost (mean
             (* n-visible
                (log (sigmoid
                      (- fe-xi-flip fe-xi)))))
       ]
    cost))

;; DEF 
(defn fit
  []
  (let [train-rbm () ; train_rbm = theano.function(
;;    [index],
;;    cost,
;;    updates=updates,
;;    givens={
;;        x: train_set_x[index * batch_size: (index + 1) * batch_size]
;;    },
;;    name='train_rbm'
;;)
        plotting-time 0
        ]
    ))

;;     # go through training epochs
;;     for epoch in xrange(training_epochs):

;;         # go through the training set
;;         mean_cost = []
;;         for batch_index in xrange(n_train_batches):
;;             mean_cost += [train_rbm(batch_index)]

;;         print 'Training epoch %d, cost is ' % epoch, numpy.mean(mean_cost)

;;         # Plot filters after each training epoch
;;         plotting_start = time.clock()
;;         # Construct image from the weight matrix
;;         image = Image.fromarray(
;;             tile_raster_images(
;;                 X=rbm.W.get_value(borrow=True).T,
;;                 img_shape=(28, 28),
;;                 tile_shape=(10, 10),
;;                 tile_spacing=(1, 1)
;;             )
;;         )
;;         image.save('filters_at_epoch_%i.png' % epoch)
;;         plotting_stop = time.clock()
;;         plotting_time += (plotting_stop - plotting_start)
;;     print ('Training took %f minutes' % (pretraining_time / 60.))

;; DEF
(defn predict
  []
  (let []))
;;     #################################
;;     #     Sampling from the RBM     #
;;     #################################
;;     # find out the number of test samples
;;     number_of_test_samples = test_set_x.get_value(borrow=True).shape[0]

;;     # pick random test examples, with which to initialize the persistent chain
;;     test_idx = rng.randint(number_of_test_samples - n_chains)
;;     persistent_vis_chain = theano.shared(
;;         numpy.asarray(
;;             test_set_x.get_value(borrow=True)[test_idx:test_idx + n_chains],
;;             dtype=theano.config.floatX
;;         )
;;     )
;; Next we create the 20 persistent chains in parallel to get our samples. To do so, we compile a theano function which performs one Gibbs step and updates the state of the persistent chain with the new visible sample. We apply this function iteratively for a large number of steps, plotting the samples at every 1000 steps.

;;     plot_every = 1000
;;     # define one step of Gibbs sampling (mf = mean-field) define a
;;     # function that does `plot_every` steps before returning the
;;     # sample for plotting
;;     (
;;         [
;;             presig_hids,
;;             hid_mfs,
;;             hid_samples,
;;             presig_vis,
;;             vis_mfs,
;;             vis_samples
;;         ],
;;         updates
;;     ) = theano.scan(
;;         rbm.gibbs_vhv,
;;         outputs_info=[None, None, None, None, None, persistent_vis_chain],
;;         n_steps=plot_every
;;     )

;;     # add to updates the shared variable that takes care of our persistent
;;     # chain :.
;;     updates.update({persistent_vis_chain: vis_samples[-1]})
;;     # construct the function that implements our persistent chain.
;;     # we generate the "mean field" activations for plotting and the actual
;;     # samples for reinitializing the state of our persistent chain
;;     sample_fn = theano.function(
;;         [],
;;         [
;;             vis_mfs[-1],
;;             vis_samples[-1]
;;         ],
;;         updates=updates,
;;         name='sample_fn'
;;     )

;;     # create a space to store the image for plotting ( we need to leave
;;     # room for the tile_spacing as well)
;;     image_data = numpy.zeros(
;;         (29 * n_samples + 1, 29 * n_chains - 1),
;;         dtype='uint8'
;;     )
;;     for idx in xrange(n_samples):
;;         # generate `plot_every` intermediate samples that we discard,
;;         # because successive samples in the chain are too correlated
;;         vis_mf, vis_sample = sample_fn()
;;         print ' ... plotting sample ', idx
;;         image_data[29 * idx:29 * idx + 28, :] = tile_raster_images(
;;             X=vis_mf,
;;             img_shape=(28, 28),
;;             tile_shape=(1, n_chains),
;;             tile_spacing=(1, 1)
;;         )

;;     # construct image
;;     image = Image.fromarray(image_data)
;;     image.save('samples.png')