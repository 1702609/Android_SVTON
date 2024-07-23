<div id="top"></div>

<h3> SVTON: SIMPLIFIED VIRTUAL TRY-ON</h3>

<p>
This repository presents the Android code for 'SVTON: SIMPLIFIED VIRTUAL TRY-ON'. 
We have included the pre-trained checkpoint and a test set.   
</p>

> **Abstract:** *We introduce a novel image-based virtual try-on model designed to replace a candidate's garment with a desired target item. The proposed model comprises three modules: segmentation, garment warping, and candidate-clothing fusion. Previous methods have shown limitations in cases involving significant differences between the original and target clothing, as well as substantial overlapping of body parts. Our model addresses these limitations by employing two key strategies. Firstly, it utilises a candidate representation based on an RGB skeleton image to enhance spatial relationships among body parts, resulting in robust segmentation and improved occlusion handling. Secondly, truncated U-Net is employed in both the segmentation and warping modules, enhancing segmentation performance and accelerating the try-on process. The warping module leverages an efficient affine transform for ease of training. Comparative evaluations against state-of-the-art models demonstrate the competitive performance of our proposed model across various scenarios, particularly excelling in handling occlusion cases and significant differences in clothing cases. This research presents a promising solution for image-based virtual try-on, advancing the field by overcoming key limitations and achieving superior performance.*

## Installation

Clone this repository:

```
git clone https://github.com/1702609/Android_SVTON
cd ./SVTON/
```

## Dataset



## Pre-trained Checkpoint


## Screenshots
![image](image/qualitative.jpg)
