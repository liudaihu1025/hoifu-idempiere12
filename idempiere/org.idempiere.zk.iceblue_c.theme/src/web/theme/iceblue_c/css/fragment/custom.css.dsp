/* 窗口工具栏：垂直布局，更紧凑尺寸 */  
.adwindow-toolbar .z-toolbarbutton.toolbarbutton-with-text {  
    flex-direction: column;  
    align-items: center;  
    justify-content: center;  
    width: auto;  
    min-width: 40px;      
    height: auto;  
    min-height: 32px;    
    padding: 1px;  
    text-align: center;  
    font-size: 10px;      
}  
.adwindow-toolbar .z-toolbarbutton.toolbarbutton-with-text .z-toolbarbutton-content {  
    flex-direction: column;  
    align-items: center;  
    justify-content: center;  
    width: auto;  
    height: auto;  
}  
.adwindow-toolbar .z-toolbarbutton.toolbarbutton-with-text [class^="z-icon-"] {  
    padding-right: 0;  
    margin-bottom: 1px;  
    font-size: 12px;      
}