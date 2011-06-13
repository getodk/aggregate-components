package org.opendatakit.tomcatutil;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ImageResize {
	
	public byte[] resizeImage(byte[] bytes, int width, int height) {

		BufferedImage image;
		try {
			image = ImageIO.read(new ByteArrayInputStream(bytes));
		} catch (IOException e1) {
			e1.printStackTrace();
			return bytes; // returns unaltered bytes
		}

		int imgWidth = image.getWidth();
		int imgHeight = image.getHeight();

		if (width <= 0 || height <= 0) {
			return bytes; // returns unaltered bytes
		}

		int reductionWidth = imgWidth / width;
		int reductionHeight = imgHeight / height;

		if (reductionWidth <= 0 || reductionHeight <= 0) {
			return bytes; // returns unaltered bytes
		}

		int reducer;
		if (reductionWidth > reductionHeight) {
			reducer = reductionWidth;
			if ((imgWidth % width) != 0) {
				reducer++;
			}
		} else {
			reducer = reductionHeight;
			if ((imgHeight % height) != 0) {
				reducer++;
			}
		}

		int resizeWidth = imgWidth / reducer;
		int resizeHeight = imgHeight / reducer;

		Image resized = image.getScaledInstance(resizeWidth, resizeHeight,
				Image.SCALE_FAST);

		ByteArrayOutputStream fileStream = new ByteArrayOutputStream();

		BufferedImage bi = new BufferedImage(resized.getWidth(null),
				resized.getHeight(null), BufferedImage.TYPE_INT_RGB);

		Graphics bg = bi.getGraphics();
		bg.drawImage(resized, 0, 0, null);
		bg.dispose();

		try {
			ImageIO.write(bi, "jpg", fileStream);
		} catch (IOException e) {
			e.printStackTrace();
			return bytes; // returns unaltered bytes
		}

		return fileStream.toByteArray();
	}

}
