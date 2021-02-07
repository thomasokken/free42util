#!/bin/sh

# This does not handle creating the Windows Free42.ico.
# That is a multi-resolution format, don't know how to deal with that.
# I used https://icoconvert.com/ on a transparent original,
# and that seems to work fine.

OPAQUE="$1"
TRANSPARENT="$2"

echo "Opaque original: $OPAQUE"
echo "Transparent original: $TRANSPARENT"

OPAQUELIST=(
120:iphone/icon.png
120:iphone/Free42/Images.xcassets/AppIcon.appiconset/icon-121.png
120:iphone/Free42/Images.xcassets/AppIcon.appiconset/icon-120.png
60:iphone/Free42/Images.xcassets/AppIcon.appiconset/icon-60.png
1024:iphone/Free42/Images.xcassets/AppIcon.appiconset/icon-1024.png
180:iphone/Free42/Images.xcassets/AppIcon.appiconset/icon-180.png
58:iphone/Free42/Images.xcassets/AppIcon.appiconset/icon-58.png
80:iphone/Free42/Images.xcassets/AppIcon.appiconset/icon-80.png
40:iphone/Free42/Images.xcassets/AppIcon.appiconset/icon-40.png
87:iphone/Free42/Images.xcassets/AppIcon.appiconset/icon-87.png
57:iphone/about-icon.png
114:iphone/about-icon@2x.png
)

TRANSPARENTLIST=(
64:mac/icon.iconset/icon_32x32@2x.png
128:mac/icon.iconset/icon_128x128.png
512:mac/icon.iconset/icon_256x256@2x.png
32:mac/icon.iconset/icon_32x32.png
512:mac/icon.iconset/icon_512x512.png
16:mac/icon.iconset/icon_16x16.png
32:mac/icon.iconset/icon_16x16@2x.png
256:mac/icon.iconset/icon_128x128@2x.png
1024:mac/icon.iconset/icon_512x512@2x.png
256:mac/icon.iconset/icon_256x256.png
96:android/app/src/main/res/drawable-xhdpi/icon.png
144:android/app/src/main/res/drawable-xxhdpi/icon.png
72:android/app/src/main/res/drawable-hdpi/icon.png
48:android/app/src/main/res/drawable-mdpi/icon.png
192:android/app/src/main/res/drawable-xxxhdpi/icon.png
)

XPMLIST=(
48:gtk/icon-48x48.xpm
128:gtk/icon-128x128.xpm
)

for TARGET in ${OPAQUELIST[@]}
do
    RES=`echo $TARGET | sed 's/^\(.*\):.*$/\1/'`
    IMG=`echo $TARGET | sed 's/^.*:\(.*\)$/\1/'`
    pngtopam "$OPAQUE" | pamscale -xysize $RES $RES | pamtopng > $IMG
done

for TARGET in ${TRANSPARENTLIST[@]}
do
    RES=`echo $TARGET | sed 's/^\(.*\):.*$/\1/'`
    IMG=`echo $TARGET | sed 's/^.*:\(.*\)$/\1/'`
    pngtopam -alphapam "$TRANSPARENT" | pamscale -xysize $RES $RES | pamtopng > $IMG
done

for TARGET in ${XPMLIST[@]}
do
    RES=`echo $TARGET | sed 's/^\(.*\):.*$/\1/'`
    IMG=`echo $TARGET | sed 's/^.*:\(.*\)$/\1/'`
    pngtopnm "$TRANSPARENT" | pnmscale -xysize $RES $RES > temp_image
    pngtopnm -alpha "$TRANSPARENT" | pnmscale -xysize $RES $RES > temp_mask
    ppmtoxpm -alphamask=temp_mask temp_image > $IMG
done

rm -f temp_image temp_mask
