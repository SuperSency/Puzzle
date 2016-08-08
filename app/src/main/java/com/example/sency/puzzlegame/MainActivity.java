package com.example.sency.puzzlegame;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MainActivity extends Activity {

    //将分割的小图片存放在一个二维数组中，三行五列
    private ImageView[][] array = new ImageView[4][4];
    private GridLayout gridView;
    private ImageView nullView;

    //手势方向
    final static int TO_TOP = 1;//往上
    final static int TO_BOTTOM = 2;//往下
    final static int TO_LEFT = 3;//往左
    final static int TO_RIGHT = 4;//往右

    //手势
    private GestureDetector mGesture;

    private boolean isGameOver;

    //判断游戏是否开始
    private boolean isStartGame;

    //判断动画是否正在执行
    private boolean isRun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        mGesture = new GestureDetector(this, new GestureListener());
        divide();
        initView();

    }

    //随机打乱顺序
    public void randomMove() {
        //打乱的次数
        for (int i = 0; i < 20; i++) {
            int type = (int) (Math.random() * 4 + 1);
            //根据手势开始交换
            changeByGesture(type, false);
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGesture.onTouchEvent(event);
    }

    //捕获到，不使用此方法某些地方手势失灵
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mGesture.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    //分割成小方块
    private void divide() {
        //获取大图
        Bitmap originPic = ((BitmapDrawable) getResources().getDrawable(R.drawable.pic)).getBitmap();
        int width = originPic.getWidth() / 4;//4列，获取每一列的宽度去设置
        int screenWidth = getWindowManager().getDefaultDisplay().getWidth() / 4;
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                //分割成正方形
                Bitmap bitmap = Bitmap.createBitmap(originPic, j * width, i * width, width, width);
                array[i][j] = new ImageView(this);
                array[i][j].setImageBitmap(bitmap);
                //适应屏幕
                array[i][j].setLayoutParams(new RelativeLayout.LayoutParams(screenWidth, screenWidth));
                //设置方块之间的间距
                array[i][j].setPadding(2, 2, 2, 2);
                array[i][j].setTag(new BundleData(i, j, bitmap));
                array[i][j].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        boolean flag = isClose((ImageView) view);
                        if (flag) {
                            changeImageByClick((ImageView) view);
                        } else {
                            Toast.makeText(MainActivity.this, "不相邻!!!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }

    //将小方块设置到布局中
    private void initView() {
        gridView = (GridLayout) findViewById(R.id.gridview);
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                gridView.addView(array[i][j]);
            }
        }
        //设置最后一个方块为空方块
        setNullImageView(array[3][3]);
        randomMove();
        isStartGame = true;
    }

    //判断手势方向
    public int gesturDir(float start_x, float start_y, float end_x, float end_y) {
        //判断是上下方向还是左右方向
        boolean dirFlag = Math.abs(start_x - end_x) > Math.abs(start_y - end_y) ? true : false;
        if (dirFlag) {
            //左右方向
            boolean isLeft = start_x - end_x > 0 ? true : false;
            if (isLeft) {
                //往左
                return TO_LEFT;
            } else {
                //往右
                return TO_RIGHT;
            }
        } else {
            //上下方向
            boolean isTop = start_y - end_y > 0 ? true : false;
            if (isTop) {
                //往上
                return TO_TOP;
            } else {
                //往下
                return TO_BOTTOM;
            }
        }
    }

    public void changeImageByClick(final ImageView imageView) {
        changeImageByClick(imageView, true);
    }

    //利用动画结束之后，交换两个方块的数据
    public void changeImageByClick(final ImageView imageView, boolean isAnima) {
        if (isRun) {
            return;
        }
        //没有动画
        if (!isAnima) {
            imageView.clearAnimation();
            BundleData mData = (BundleData) imageView.getTag();
            nullView.setImageBitmap(mData.bitmap);
            BundleData nullData = (BundleData) nullView.getTag();
            nullData.bitmap = mData.bitmap;
            nullData.p_x = mData.p_x;
            nullData.p_y = mData.p_y;
            setNullImageView(imageView);
            if (isStartGame) {
                isGameOver();
            }
            return;
        }
        //创建一个动画，设置好方向，移动的距离
        TranslateAnimation animation = null;
        if (imageView.getX() > nullView.getX()) {
            //如果在空方块的右边则往左移动
            animation = new TranslateAnimation(0.1f, -imageView.getWidth(), 0.1f, 0.1f);
        } else if (imageView.getX() < nullView.getX()) {
            //如果在空方块的左边则往右移动
            animation = new TranslateAnimation(0.1f, imageView.getWidth(), 0.1f, 0.1f);
        } else if (imageView.getY() > nullView.getY()) {
            //如果在空方块的下边则往上移动
            animation = new TranslateAnimation(0.1f, 0.1f, 0.1f, -imageView.getWidth());
        } else if (imageView.getY() < nullView.getY()) {
            //如果在空方块的上边则往下移动
            animation = new TranslateAnimation(0.1f, 0.1f, 0.1f, imageView.getWidth());
        }
           //设置动画的时长
            animation.setDuration(70);
            //设置动画结束之后是否停留
            animation.setFillAfter(true);
            //设置动画结束之后要真正的把数据交换
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    isRun = true;
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    isRun = false;
                    imageView.clearAnimation();
                    BundleData mData = (BundleData) imageView.getTag();
                    nullView.setImageBitmap(mData.bitmap);
                    BundleData nullData = (BundleData) nullView.getTag();
                    nullData.bitmap = mData.bitmap;
                    nullData.p_x = mData.p_x;
                    nullData.p_y = mData.p_y;
                    setNullImageView(imageView);
                    if (isStartGame) {
                        isGameOver();
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            //执行动画
            imageView.startAnimation(animation);
        }


    //默认有动画
    public void changeByGesture(int type) {
        changeByGesture(type, true);
    }

    //是否有动画
    public void changeByGesture(int type, boolean isAnima) {
        //获取当前空方块的位置
       // Log.i("tag","GestureNullData");
        BundleData nullData = (BundleData) nullView.getTag();
        //根据方向，设置相应的相邻位置的坐标
        int new_x = nullData.true_X;
        int new_y = nullData.true_Y;
        if (type == 1) {
            //往上
            new_x++;
        } else if (type == 2) {
            //往下
            new_x--;
        } else if (type == 3) {
            //往左
            new_y++;
        } else if (type == 4) {
            //往右
            new_y--;
        }
        //判断这个新坐标,是否存在
        if (new_x >=0 && new_x < array.length && new_y >= 0 && new_y < array[0].length) {
            if (isAnima) {
                //动画交换
                //存在的话，开始移动
                changeImageByClick(array[new_x][new_y]);
            } else {
                //直接交换
                changeImageByClick(array[new_x][new_y], false);
            }
        }
    }

    //设置某个方块为空方块
    public void setNullImageView(ImageView imageView) {
        imageView.setImageBitmap(null);
        this.nullView = imageView;
    }

    //判断当前点击的方块是否与空方块相邻
    public boolean isClose(ImageView imageView) {
        //分别获取当前空方块的位置与点击方块的位置，通过true_x,true_y两边都相差1
        BundleData nullData = (BundleData) nullView.getTag();
        BundleData mData = (BundleData) imageView.getTag();
        /**通过true而不是px判断
         *因为当nullData和mData交换之后
         * 当前图片的px与要设置为空图片的px相等了
         * 只有true值是还存在差值的
         */
        if (mData.true_Y == nullData.true_Y && mData.true_X + 1 == nullData.true_X) {
            //当前点击的方块在空方块上边
            return true;
        } else if (mData.true_Y == nullData.true_Y && mData.true_X - 1 == nullData.true_X) {
            //当前点击的方块在空方块下边
            return true;
        } else if (mData.true_Y + 1 == nullData.true_Y && mData.true_X == nullData.true_X) {
            //当前点击的方块在空方块左边
            return true;
        } else if (mData.true_Y - 1 == nullData.true_Y && mData.true_X == nullData.true_X) {
            //当前点击的方块在空方块右边
            return true;
        }
        return false;
    }

    //每个游戏小方块上要绑定的数据
    class BundleData {
        //每个小方块的实际位置X
        public int true_X = 0;
        //每个小方块的实际位置Y
        public int true_Y = 0;
        //每个小方块的图片
        public Bitmap bitmap;
        //每个小方块图片当前的位置X
        public int p_x = 0;
        //每个小方块图片当前的位置Y
        public int p_y = 0;

        public BundleData(int true_X, int true_Y, Bitmap bitmap) {
            this.true_X = true_X;
            this.true_Y = true_Y;
            this.bitmap = bitmap;
            this.p_x = true_X;
            this.p_y = true_Y;
        }

        //判断游戏是否结束
        public boolean isTrue() {
            if (true_X == p_x && true_Y == p_y) {
                return true;
            } else {
                return false;
            }
        }
    }

    private class GestureListener implements GestureDetector.OnGestureListener {
        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            int type = gesturDir(motionEvent.getX(), motionEvent.getY()
                    , motionEvent1.getX(), motionEvent1.getY());
           // Log.i("tag", "Director" + type);
            changeByGesture(type);
            return false;
        }
    }

    //判断游戏结束
    public void isGameOver() {
        //遍历每个小方块
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                //空方块不判断
                if (array[i][j] == nullView) {
                    continue;
                }
                BundleData mData = (BundleData) array[i][j].getTag();
                if (!mData.isTrue()) {
                    isGameOver = false;
                    break;
                } else {
                    isGameOver = true;
                }
            }
        }

        //根据一个开关变量决定游戏是否结束，结束时给出提示
        if (isGameOver) {
            Toast.makeText(MainActivity.this, "成功了！！！", Toast.LENGTH_SHORT).show();
        }
    }
}
