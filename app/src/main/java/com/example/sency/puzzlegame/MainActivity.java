package com.example.sency.puzzlegame;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends Activity implements View.OnClickListener {

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

    //判断游戏是否开始
    private boolean isStartGame;

    //判断动画是否正在执行
    private boolean isRun;

    //控件
    private TextView mStep;
    private TextView mLevel;
    private Chronometer mTime;
    private Button mSelect;

    private FloatingActionButton mMenu;
    private FloatingActionButton mRestart;
    private FloatingActionButton mQiut;
    private RelativeLayout layout;
    //是否暂停了
    private boolean mFlag = false;
    private long rangeTime;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    // private boolean clickFlag = true;

    final static int CHOOSE_PHOTO = 1;
    final static int CROP_PHOTO = 2;

    private int steps = 0;
    static int levels = 1;
    int screenWidth;
    Bitmap originPic;
    boolean picFlag = false;
    boolean firstFlag = true;

    Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        mGesture = new GestureDetector(this, new GestureListener());
        divide();
        initView();
        if (!mFlag) {
            mSelect.setOnClickListener(this);
        }
        mMenu.setOnClickListener(this);
        mQiut.setOnClickListener(this);
        mRestart.setOnClickListener(this);
    }

    //分割成小方块
    private void divide() {
        //获取大图
        if (!picFlag) {
            originPic = ((BitmapDrawable) getResources().getDrawable(R.drawable.pic2)).getBitmap();
        }
        int width = originPic.getWidth() / 4;//4列，获取每一列的宽度去设置
        screenWidth = getWindowManager().getDefaultDisplay().getWidth() / 4;
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
                        if (!mFlag) {
                            boolean flag = isClose((ImageView) view);
                            if (flag) {
                                changeImageByClick((ImageView) view);
                            } else {
                                Toast.makeText(MainActivity.this, "不相邻!!!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            }
        }
    }

    //将小方块设置到布局中
    private void initView() {
        mStep = (TextView) findViewById(R.id.step);
        mLevel = (TextView) findViewById(R.id.level);
        mTime = (Chronometer) findViewById(R.id.time);

        mSelect = (Button) findViewById(R.id.select);

        mMenu = (FloatingActionButton) findViewById(R.id.menu);
        mRestart = (FloatingActionButton) findViewById(R.id.restart);
        mQiut = (FloatingActionButton) findViewById(R.id.qiut);
        layout = (RelativeLayout) findViewById(R.id.layout);

        mRestart.hide();
        mQiut.hide();

        gridView = (GridLayout) findViewById(R.id.gridview);
        initGridView();
        initData();
    }

    private void initGridView() {
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                gridView.addView(array[i][j]);
            }
        }
    }

    private void initData() {
        isStartGame = false;
        //设置最后一个方块为空方块
        setNullImageView(array[3][3]);
        randomMove();
        mLevel.setText(levels+"");
        //清零
        mTime.setBase(SystemClock.elapsedRealtime());
        //开始计时
        mTime.start();
        steps = 0;
        mStep.setText(steps + "");
        isStartGame = true;

    }

    private void resetData() {
        isStartGame = false;
        randomMove();
        //清零
        mTime.setBase(SystemClock.elapsedRealtime());
        //开始计时
        mTime.start();
        steps = 0;
        mStep.setText(steps + "");
        isStartGame = true;
    }


    //随机打乱顺序
    public void randomMove() {
        boolean flag = true;
        //Log.i("tag", "levels:" + levels);
        if (firstFlag) {
            preferences = getSharedPreferences("levelData", MODE_PRIVATE);
            levels = preferences.getInt("levels", 1);
            firstFlag = false;
        }
        //打乱的次数
        for (int i = 0; i < (7 * levels); i++) {
            int type = (int) (Math.random() * 4 + 1);
            //根据手势开始交换
            changeByGesture(type, false);
        }
        while (flag) {
            for (int i = 0; i < array.length; i++) {
                BundleData mData = null;
                for (int j = 0; j < array[0].length; j++) {
                    //空方块不判断
                    if (array[i][j] == nullView) {
                        continue;
                    }
                    mData = (BundleData) array[i][j].getTag();
                    if (!mData.isTrue()) {
                        flag = false;
                        break;
                    }
                }
                if (!mData.isTrue()) {
                    flag = false;
                    break;
                }
                int type = (int) (Math.random() * 4 + 1);
                //根据手势开始交换
                changeByGesture(type, false);
            }
        }

    }

    //手势监听
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
                steps++;
                mStep.setText(steps + "");
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
                    steps++;
                    mStep.setText(steps + "");
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
        if (new_x >= 0 && new_x < array.length && new_y >= 0 && new_y < array[0].length) {
            if (!mFlag) {
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

    //选择相册
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.select:
                getPhoto();
                break;
            case R.id.menu:
                if (!mFlag) {
                    mTime.stop();
                    rangeTime = SystemClock.elapsedRealtime() - mTime.getBase();
                    //逆时针方向
                    ObjectAnimator animator = ObjectAnimator.ofFloat(mMenu, "rotation", 0f, 45f);
                    animator.setDuration(200);
                    animator.start();
                    mRestart.show();
                    mQiut.show();
                    //朦胧
                    layout.setAlpha(0.5f);
                    mFlag = true;
                } else {
                    gridView.setFocusable(true);
                    mTime.setBase(SystemClock.elapsedRealtime() - rangeTime);
                    mTime.start();
                    ObjectAnimator animator = ObjectAnimator.ofFloat(mMenu, "rotation", 45f, 0f);
                    animator.setDuration(500);
                    animator.start();
                    mRestart.hide();
                    mQiut.hide();
                    layout.setAlpha(1);
                    mFlag = false;
                }
                break;
            case R.id.restart:
                ObjectAnimator animator = ObjectAnimator.ofFloat(mMenu, "rotation", 45f, 0f);
                animator.setDuration(500);
                animator.start();
                mRestart.hide();
                mQiut.hide();
                mFlag = false;
                resetData();
                layout.setAlpha(1);
                break;
            case R.id.qiut:
                editor = getSharedPreferences("levelData", MODE_PRIVATE).edit();
                editor.putInt("levels", levels);
                editor.commit();
                finish();
                break;
        }
    }

    //获取手机图片
    private void getPhoto() {
        Intent intent = new Intent();
        //设置为图片类型
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, CHOOSE_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CHOOSE_PHOTO:
                    uri = data.getData();
                    ContentResolver cr = this.getContentResolver();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(cr, uri);
                        //裁剪
                        Bitmap bm = Bitmap.createScaledBitmap(bitmap, screenWidth, screenWidth, true);
                        picFlag = true;
                        originPic = bm;
                        gridView.removeAllViews();
                        divide();
                        initGridView();
                        initData();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case CROP_PHOTO:
                    break;
                default:
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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
            Log.i("tag", "Data:[" + true_X + "," + true_Y + "]" + "[" + p_x + "," + p_y + "]");
        }

        //判断游戏是否结束
        public boolean isTrue() {
            if (true_X == p_x && true_Y == p_y) {
                Log.i("tag", "-------------------------------------");
                Log.i("tag", "true---------" + levels);
                Log.i("tag", "Data:[" + true_X + "," + true_Y + "]" + "[" + p_x + "," + p_y + "]");
                Log.i("tag", "-------------------------------------");
                return true;
            } else {
                Log.i("tag", "false---------" + levels);
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
        boolean isGameOver = true;
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
                }
            }
            if (!isGameOver) {
                break;
            }
        }

        //根据一个开关变量决定游戏是否结束，结束时给出提示
        if (isGameOver) {
            //停止计时
            mTime.stop();
            //保存记录
            editor = getSharedPreferences("levelData", MODE_PRIVATE).edit();
            editor.putInt("levels", (levels+1));
            editor.commit();

            Toast.makeText(MainActivity.this, "成功了！！！", Toast.LENGTH_SHORT).show();
            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
            dialog.setTitle("恭喜你,成功了!!!");
            dialog.setMessage("是否进入下一关?");
            dialog.setCancelable(false);
            dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    firstFlag = false;
                    levels++;
                    mLevel.setText(levels + "");
                    initData();
                }
            });
            dialog.setNegativeButton("Cancle", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            dialog.show();
        }
    }


}
