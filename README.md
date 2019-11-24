# TensorFlow Lite Demo

TensorFlow Lite usage demonstration project on Android applied to the classification of images of dogs and cats. In the repository is also attached a MNIST model and CIFAR10 model in order to play with various basic models.

<ul>
    <li>MNIST: Classifies images or drawings of numbers between 0 and 9.</li>
    <li>CIFAR10: Classifies images or drawings of airplanes, automobiles, birds, cats, deers, dogs, frogs, horses, ships and trucks</li>
</ul>

The application demo is based on the classification of random images of cats and dogs extracted from the internet using The Cat API and The Dog API. 

<ul><li><b>Before compiling and running the demo</b>, you must register for an API key in https://thecatapi.com/signup and in https://thedogapi.com/signup in order to fetch cat and dog images and use it as a @Header in the GET petition in RetrofitInstance.kt.</li></ul>

## Loading the model

In order to load a model, you must move or copy the model to the assets folder and modify various variables in Classifier.kt depending on the model (in order to use MNIST or CIFAR10, the application may need changes):

### Cats-Vs-Dogs

    INPUT_HEIGHT = INPUT_WIDTH = 75
    Number of classes = 2

### MNIST

    INPUT_HEIGHT = INPUT_WIDTH = 28
    Number of classes = 10

### CIFAR10

    INPUT_HEIGHT = INPUT_WIDTH = 32
    Number of classes = 10
